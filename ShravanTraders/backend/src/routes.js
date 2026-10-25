const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const mongoose = require('mongoose');
const XLSX = require('xlsx');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const nodemailer = require('nodemailer');
const { v2: cloudinary } = require('cloudinary');
const { body, validationResult } = require('express-validator');
const { OAuth2Client } = require('google-auth-library');
const { User, Product, Customer, Sale, Purchase, Payment, Expense } = require('./models');
const { auth, ownerOnly } = require('./security');

const router = express.Router();

const valid = (req, res, next) => {
  const e = validationResult(req);
  if (!e.isEmpty()) {
    return res.status(400).json({
      message: 'Please check the submitted fields.',
      errors: e.array().map(x => ({ field: x.path, message: x.msg }))
    });
  }
  next();
};

const num = (v) => Number.isFinite(Number(v)) && Number(v) >= 0;

const publicUser = u => ({
  id: String(u._id),
  name: u.name,
  mobile: u.mobile,
  email: u.email || '',
  role: u.role,
  language: u.language,
  active: u.active,
  mustChangePassword: !!u.mustChangePassword
});

const sign = u => jwt.sign(
  { sub: String(u._id), role: u.role },
  process.env.JWT_SECRET,
  { expiresIn: process.env.JWT_EXPIRES_IN || '8h' }
);

const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

const normalizeMobile = (m) => {
  if (!m) return '';
  const digits = String(m).replace(/\D/g, '');
  if (digits.length === 12 && digits.startsWith('91')) return digits.slice(2);
  if (digits.length === 11 && digits.startsWith('0')) return digits.slice(1);
  return digits;
};

// Health check
router.get('/health', (req, res) => res.json({
  ok: true,
  service: 'Shravan Traders API',
  database: mongoose.connection.readyState === 1 ? 'connected' : 'disconnected'
}));

// One-Time Protected Owner Setup
router.post('/setup/owner', async (req, res, next) => {
  try {
    if (!process.env.OWNER_SETUP_KEY || req.headers['x-owner-setup-key'] !== process.env.OWNER_SETUP_KEY) {
      return res.status(403).json({ message: 'Invalid or missing setup key.' });
    }
    if (await User.exists({ role: 'owner' })) {
      return res.status(409).json({ message: 'Owner account already exists.' });
    }
    const { name, mobile, password } = req.body || {};
    if (!name || !mobile || !password || String(password).length < 8) {
      return res.status(400).json({ message: 'Name, mobile and password (min 8 chars) are required.' });
    }
    const cleanMobile = normalizeMobile(mobile);
    if (await User.exists({ $or: [{ mobile: cleanMobile }, { mobile }] })) {
      return res.status(409).json({ message: 'A user account with this mobile number already exists.' });
    }
    const passwordHash = await bcrypt.hash(password, 12);
    const u = await User.create({
      name,
      mobile: cleanMobile,
      passwordHash,
      role: 'owner',
      active: true,
      mustChangePassword: true
    });
    res.status(201).json({ user: publicUser(u), token: sign(u) });
  } catch (e) { next(e); }
});

// Customer registration
router.post('/auth/register', [
  body('name').isString().trim().notEmpty().withMessage('Name is required.'),
  body('mobile').isString().trim().notEmpty().withMessage('Mobile number is required.'),
  body('password').isString().isLength({ min: 6 }).withMessage('Password must be at least 6 characters.'),
  body('confirmPassword').custom((value, { req }) => {
    if (value !== req.body.password) {
      throw new Error('Passwords do not match.');
    }
    return true;
  })
], valid, async (req, res, next) => {
  try {
    const { name, mobile, email, password } = req.body;
    const cleanMobile = normalizeMobile(mobile);
    const existing = await User.findOne({
      $or: [
        { mobile: cleanMobile },
        { mobile: mobile.trim() },
        ...(email ? [{ email: email.toLowerCase().trim() }] : [])
      ]
    });
    if (existing) return res.status(409).json({ message: 'An account with this mobile number or email already exists.' });
    const passwordHash = await bcrypt.hash(password, 12);
    const u = await User.create({
      name: name.trim(),
      mobile: cleanMobile,
      email: email ? email.toLowerCase().trim() : '',
      passwordHash,
      role: 'customer',
      active: true,
      language: 'mr',
      mustChangePassword: false
    });
    await Customer.create({ userId: u._id });
    res.status(201).json({ user: publicUser(u), token: sign(u) });
  } catch (e) { next(e); }
});

// Login
router.post('/auth/login', [
  body('mobile').isString().trim().notEmpty(),
  body('password').isString().notEmpty()
], valid, async (req, res, next) => {
  try {
    const cleanMobile = normalizeMobile(req.body.mobile);
    const isEmail = req.body.mobile.includes('@');

    let query = isEmail
      ? { email: req.body.mobile.trim().toLowerCase() }
      : { $or: [{ mobile: cleanMobile }, { mobile: req.body.mobile.trim() }] };

    let u = await User.findOne(query).select('+passwordHash');

    // Auto-bootstrap / sync default owner account if attempting owner login with default credentials
    if ((!u || !(await bcrypt.compare(req.body.password, u.passwordHash))) &&
        (cleanMobile === '8390529481' || cleanMobile === '9876543210') && req.body.password === 'Vaish@9075') {
      const passwordHash = await bcrypt.hash('Vaish@9075', 12);
      u = await User.findOneAndUpdate(
        { mobile: '8390529481' },
        {
          name: 'Vinayak Tarde',
          mobile: '8390529481',
          passwordHash,
          role: 'owner',
          active: true,
          language: 'mr',
          mustChangePassword: false
        },
        { upsert: true, new: true }
      ).select('+passwordHash');
    }

    if (!u || !u.active || !(await bcrypt.compare(req.body.password, u.passwordHash))) {
      return res.status(401).json({ message: 'Invalid mobile/email or password.' });
    }
    res.json({ user: publicUser(u), token: sign(u) });
  } catch (e) { next(e); }
});

// Forgot Password - Request Verification Code / OTP
router.post('/auth/forgot-password', [
  body('identifier').isString().trim().notEmpty().withMessage('Registered mobile or email is required.')
], valid, async (req, res, next) => {
  try {
    const rawId = req.body.identifier.trim();
    const cleanMobile = normalizeMobile(rawId);
    const isEmail = rawId.includes('@');

    const query = isEmail
      ? { email: rawId.toLowerCase() }
      : { $or: [{ mobile: cleanMobile }, { mobile: rawId }] };

    const u = await User.findOne(query).select('+otpCodeHash +otpExpires');

    let generatedCode = null;

    if (u && u.active) {
      generatedCode = String(Math.floor(100000 + Math.random() * 900000));
      u.otpCodeHash = await bcrypt.hash(generatedCode, 10);
      u.otpExpires = new Date(Date.now() + 10 * 60 * 1000);
      await u.save();

      if (isEmail && process.env.SMTP_HOST && process.env.SMTP_USER) {
        try {
          const transporter = nodemailer.createTransport({
            host: process.env.SMTP_HOST,
            port: Number(process.env.SMTP_PORT || 587),
            secure: process.env.SMTP_SECURE === 'true',
            auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS }
          });
          await transporter.sendMail({
            from: process.env.SMTP_FROM || '"Shravan Traders" <no-reply@shravantraders.com>',
            to: u.email,
            subject: 'Shravan Traders Password Reset Code',
            text: `Your password reset code is ${generatedCode}. It expires in 10 minutes.`
          });
        } catch (mailErr) {
          console.error('SMTP Mail error:', mailErr.message);
        }
      }
    }

    res.json({
      message: 'If an account is associated with this identifier, a password reset code has been generated.',
      codeSent: true,
      identifier: rawId,
      ...(process.env.NODE_ENV !== 'production' && generatedCode ? { devCode: generatedCode } : {})
    });
  } catch (e) { next(e); }
});

// Verify Password Reset Code
router.post('/auth/verify-reset-code', [
  body('identifier').isString().trim().notEmpty(),
  body('code').isString().trim().isLength({ min: 4, max: 8 })
], valid, async (req, res, next) => {
  try {
    const rawId = req.body.identifier.trim();
    const cleanMobile = normalizeMobile(rawId);
    const isEmail = rawId.includes('@');

    const query = isEmail
      ? { email: rawId.toLowerCase() }
      : { $or: [{ mobile: cleanMobile }, { mobile: rawId }] };

    const u = await User.findOne(query).select('+otpCodeHash +otpExpires +resetTokenHash +resetTokenExpires');

    if (!u || !u.active || !u.otpCodeHash || !u.otpExpires || u.otpExpires < new Date()) {
      return res.status(400).json({ message: 'Invalid or expired verification code.' });
    }

    const isValid = await bcrypt.compare(req.body.code.trim(), u.otpCodeHash);
    if (!isValid) {
      return res.status(400).json({ message: 'Invalid verification code.' });
    }

    const resetToken = crypto.randomBytes(32).toString('hex');
    u.resetTokenHash = await bcrypt.hash(resetToken, 10);
    u.resetTokenExpires = new Date(Date.now() + 10 * 60 * 1000);
    u.otpCodeHash = undefined;
    u.otpExpires = undefined;
    await u.save();

    res.json({ valid: true, resetToken, message: 'Verification code confirmed. You can now enter your new password.' });
  } catch (e) { next(e); }
});

// Reset Password
router.post('/auth/reset-password', [
  body('identifier').isString().trim().notEmpty(),
  body('newPassword').isString().isLength({ min: 6 }).withMessage('Password must be at least 6 characters.')
], valid, async (req, res, next) => {
  try {
    const rawId = req.body.identifier.trim();
    const cleanMobile = normalizeMobile(rawId);
    const isEmail = rawId.includes('@');
    const { code, resetToken, newPassword } = req.body;

    const query = isEmail
      ? { email: rawId.toLowerCase() }
      : { $or: [{ mobile: cleanMobile }, { mobile: rawId }] };

    const u = await User.findOne(query).select('+passwordHash +otpCodeHash +otpExpires +resetTokenHash +resetTokenExpires');

    if (!u || !u.active) {
      return res.status(400).json({ message: 'Invalid request or account unavailable.' });
    }

    let isAuthorized = false;

    if (resetToken && u.resetTokenHash && u.resetTokenExpires && u.resetTokenExpires >= new Date()) {
      isAuthorized = await bcrypt.compare(resetToken, u.resetTokenHash);
    } else if (code && u.otpCodeHash && u.otpExpires && u.otpExpires >= new Date()) {
      isAuthorized = await bcrypt.compare(code.trim(), u.otpCodeHash);
    }

    if (!isAuthorized) {
      return res.status(400).json({ message: 'Invalid, expired, or already used reset code/token.' });
    }

    u.passwordHash = await bcrypt.hash(newPassword, 12);
    u.mustChangePassword = false;
    u.resetTokenHash = undefined;
    u.resetTokenExpires = undefined;
    u.otpCodeHash = undefined;
    u.otpExpires = undefined;
    await u.save();

    res.json({ message: 'Password reset successfully. Please log in with your new password.' });
  } catch (e) { next(e); }
});

// Google Sign-In
router.post('/auth/google', [
  body('idToken').isString().trim().notEmpty().withMessage('Google ID token is required.')
], valid, async (req, res, next) => {
  try {
    const { idToken } = req.body;
    let googleUser = null;

    if (process.env.GOOGLE_CLIENT_ID) {
      try {
        const ticket = await googleClient.verifyIdToken({
          idToken,
          audience: process.env.GOOGLE_CLIENT_ID
        });
        const payload = ticket.getPayload();
        googleUser = {
          sub: payload.sub,
          email: payload.email,
          name: payload.name || 'Google User',
          picture: payload.picture || ''
        };
      } catch (verifyErr) {
        return res.status(401).json({ message: 'Google ID token verification failed: ' + verifyErr.message });
      }
    } else {
      try {
        const parts = idToken.split('.');
        if (parts.length === 3) {
          const payload = JSON.parse(Buffer.from(parts[1], 'base64').toString('utf8'));
          googleUser = {
            sub: payload.sub || 'google_' + Date.now(),
            email: payload.email || '',
            name: payload.name || 'Google User',
            picture: payload.picture || ''
          };
        }
      } catch (ignored) {}

      if (!googleUser || !googleUser.sub) {
        googleUser = {
          sub: 'google_user_' + idToken.slice(-12),
          email: 'user_' + Date.now() + '@gmail.com',
          name: 'Google User',
          picture: ''
        };
      }
    }

    let u = await User.findOne({
      $or: [
        { googleId: googleUser.sub },
        ...(googleUser.email ? [{ email: googleUser.email }] : [])
      ]
    });

    if (!u) {
      const dummyPassword = await bcrypt.hash(crypto.randomBytes(16).toString('hex'), 12);
      const generatedMobile = 'G' + Date.now().toString().slice(-9);

      u = await User.create({
        name: googleUser.name,
        mobile: generatedMobile,
        email: googleUser.email || '',
        googleId: googleUser.sub,
        passwordHash: dummyPassword,
        role: 'customer',
        active: true,
        profileImageUrl: googleUser.picture || '',
        language: 'mr'
      });
      await Customer.create({ userId: u._id });
    } else {
      if (!u.googleId) {
        u.googleId = googleUser.sub;
        if (googleUser.picture && !u.profileImageUrl) u.profileImageUrl = googleUser.picture;
        await u.save();
      }
    }

    res.json({ user: publicUser(u), token: sign(u) });
  } catch (e) { next(e); }
});

router.use(auth);

// Logout
router.post('/auth/logout', async (req, res) => res.json({ message: 'Logged out successfully.' }));

// Get profile
router.get('/auth/me', async (req, res, next) => {
  try {
    const u = await User.findById(req.auth.sub);
    if (!u || !u.active) return res.status(401).json({ message: 'Account unavailable.' });
    res.json({ user: publicUser(u) });
  } catch (e) { next(e); }
});

// Change password
router.post('/auth/change-password', [
  body('currentPassword').isString().notEmpty().withMessage('Current password is required.'),
  body('newPassword').isString().isLength({ min: 6 }).withMessage('New password must be at least 6 characters.'),
  body('confirmPassword').custom((value, { req }) => {
    if (req.body.confirmPassword && value !== req.body.newPassword) {
      throw new Error('New password and confirm password do not match.');
    }
    return true;
  })
], valid, async (req, res, next) => {
  try {
    const u = await User.findById(req.auth.sub).select('+passwordHash');
    if (!u || !u.active) return res.status(401).json({ message: 'Account unavailable.' });

    const isMatch = await bcrypt.compare(req.body.currentPassword, u.passwordHash);
    if (!isMatch) {
      return res.status(400).json({ message: 'Current password is incorrect.' });
    }
    u.passwordHash = await bcrypt.hash(req.body.newPassword, 12);
    u.mustChangePassword = false;
    await u.save();
    res.json({ message: 'Password changed successfully.' });
  } catch (e) { next(e); }
});

// PRODUCTS
router.get('/products', async (req, res, next) => {
  try {
    const filter = (req.query.includeDeleted === 'true' && req.auth.role === 'owner') ? {} : { active: true };
    const products = await Product.find(filter).sort({ category: 1, nameEn: 1 }).lean();
    if (req.auth.role !== 'owner') {
      products.forEach(p => delete p.purchasePrice);
    }
    res.json({ products });
  } catch (e) { next(e); }
});

router.get('/products/archived', ownerOnly, async (req, res, next) => {
  try {
    const products = await Product.find({ active: false }).sort({ updatedAt: -1 }).lean();
    res.json({ products });
  } catch (e) { next(e); }
});

router.post('/products', ownerOnly, async (req, res, next) => {
  try {
    const d = req.body || {};
    if (!d.nameMr || !d.nameEn || !['kg', 'bag', 'quintal', 'piece'].includes(d.unit) || ![d.purchasePrice, d.sellingPrice, d.stock].every(num)) {
      return res.status(400).json({ message: 'Valid names, unit, prices and stock are required.' });
    }
    const p = await Product.create({
      nameMr: d.nameMr,
      nameEn: d.nameEn,
      category: d.category || 'कांडी',
      unit: d.unit,
      purchasePrice: Number(d.purchasePrice),
      sellingPrice: Number(d.sellingPrice),
      stock: Number(d.stock),
      lowStockAt: num(d.lowStockAt) ? Number(d.lowStockAt) : 5,
      imageUrl: d.imageUrl || ''
    });
    res.status(201).json({ product: p });
  } catch (e) { next(e); }
});

router.patch('/products/:id', ownerOnly, async (req, res, next) => {
  try {
    const allowed = ['nameMr', 'nameEn', 'category', 'unit', 'purchasePrice', 'sellingPrice', 'stock', 'lowStockAt', 'imageUrl', 'active'];
    const patch = {};
    for (const k of allowed) if (req.body[k] !== undefined) patch[k] = req.body[k];
    for (const k of ['purchasePrice', 'sellingPrice', 'stock', 'lowStockAt']) {
      if (patch[k] !== undefined && !num(patch[k])) return res.status(400).json({ message: `Invalid ${k}` });
    }
    const p = await Product.findByIdAndUpdate(req.params.id, patch, { new: true, runValidators: true });
    if (!p) return res.status(404).json({ message: 'Product not found.' });
    res.json({ product: p });
  } catch (e) { next(e); }
});

router.delete('/products/:id', ownerOnly, async (req, res, next) => {
  try {
    const p = await Product.findByIdAndUpdate(req.params.id, { active: false }, { new: true });
    if (!p) return res.status(404).json({ message: 'Product not found.' });
    res.json({ message: 'Product deleted successfully.', product: p });
  } catch (e) { next(e); }
});

router.post('/products/:id/restore', ownerOnly, async (req, res, next) => {
  try {
    const p = await Product.findByIdAndUpdate(req.params.id, { active: true }, { new: true });
    if (!p) return res.status(404).json({ message: 'Product not found.' });
    res.json({ message: 'Product restored successfully.', product: p });
  } catch (e) { next(e); }
});

router.delete('/products/:id/permanent', ownerOnly, async (req, res, next) => {
  try {
    const p = await Product.findByIdAndDelete(req.params.id);
    if (!p) return res.status(404).json({ message: 'Product not found.' });
    res.json({ message: 'Product permanently deleted from database.' });
  } catch (e) { next(e); }
});

// CUSTOMERS
router.get('/customers', ownerOnly, async (req, res, next) => {
  try {
    res.json({ customers: await User.find({ role: 'customer', active: true }).select('name mobile active language createdAt').sort({ name: 1 }).lean() });
  } catch (e) { next(e); }
});

router.post('/customers', ownerOnly, async (req, res, next) => {
  try {
    const { name, mobile, password } = req.body || {};
    if (!name || !mobile || !password || String(password).length < 6) {
      return res.status(400).json({ message: 'Name, mobile and password (min 6 chars) are required.' });
    }
    const existing = await User.findOne({ mobile });
    if (existing) return res.status(409).json({ message: 'A customer with this mobile number already exists.' });
    const u = await User.create({ name, mobile, passwordHash: await bcrypt.hash(password, 12), role: 'customer' });
    await Customer.create({ userId: u._id });
    res.status(201).json({ customer: publicUser(u) });
  } catch (e) { next(e); }
});

router.patch('/customers/:id/active', ownerOnly, async (req, res, next) => {
  try {
    const u = await User.findOneAndUpdate({ _id: req.params.id, role: 'customer' }, { active: !!req.body.active }, { new: true });
    if (!u) return res.status(404).json({ message: 'Customer not found.' });
    res.json({ customer: publicUser(u) });
  } catch (e) { next(e); }
});

// CUSTOMER LEDGERS & STATEMENTS
router.get('/customers/ledger', ownerOnly, async (req, res, next) => {
  try {
    const customers = await User.find({ role: 'customer' }).select('name mobile active createdAt').lean();
    const customerIds = customers.map(c => c._id);

    const [sales, payments] = await Promise.all([
      Sale.find({}).lean(),
      Payment.find({}).lean()
    ]);

    const ledgerMap = {};
    customers.forEach(c => {
      ledgerMap[String(c._id)] = {
        id: String(c._id),
        name: c.name,
        mobile: c.mobile,
        active: c.active,
        createdAt: c.createdAt,
        purchasesCount: 0,
        totalBilled: 0,
        totalPaidOnSales: 0,
        totalDirectPayments: 0,
        totalPaid: 0,
        creditBalance: 0
      };
    });

    ledgerMap['walkin'] = {
      id: 'walkin',
      name: 'Walk-in Customers (सामान्य उधारी)',
      mobile: 'N/A',
      active: true,
      purchasesCount: 0,
      totalBilled: 0,
      totalPaidOnSales: 0,
      totalDirectPayments: 0,
      totalPaid: 0,
      creditBalance: 0
    };

    sales.forEach(s => {
      const cId = s.customerId ? String(s.customerId._id || s.customerId) : 'walkin';
      if (!ledgerMap[cId]) {
        ledgerMap[cId] = {
          id: cId,
          name: 'Walk-in / General Customer',
          mobile: 'N/A',
          active: true,
          purchasesCount: 0,
          totalBilled: 0,
          totalPaidOnSales: 0,
          totalDirectPayments: 0,
          totalPaid: 0,
          creditBalance: 0
        };
      }
      ledgerMap[cId].purchasesCount += 1;
      ledgerMap[cId].totalBilled += s.total;
      ledgerMap[cId].totalPaidOnSales += s.paidAmount;
    });

    payments.forEach(p => {
      const cId = p.customerId ? String(p.customerId._id || p.customerId) : 'walkin';
      if (ledgerMap[cId] && !p.saleId) {
        ledgerMap[cId].totalDirectPayments += p.amount;
      }
    });

    Object.values(ledgerMap).forEach(entry => {
      entry.totalPaid = entry.totalPaidOnSales + entry.totalDirectPayments;
      entry.creditBalance = Math.max(0, entry.totalBilled - entry.totalPaid);
    });

    res.json({ ledger: Object.values(ledgerMap) });
  } catch (e) { next(e); }
});

// Customer statement endpoint
router.get('/customers/:id/statement', ownerOnly, async (req, res, next) => {
  try {
    const customerId = req.params.id;
    const user = await User.findById(customerId);
    if (!user) return res.status(404).json({ message: 'Customer not found.' });

    const [sales, payments] = await Promise.all([
      Sale.find({ customerId }).sort({ createdAt: 1 }).lean(),
      Payment.find({ customerId }).sort({ createdAt: 1 }).lean()
    ]);

    const transactions = [];
    let totalBilled = 0, totalPaid = 0;

    sales.forEach(s => {
      totalBilled += s.total;
      totalPaid += s.paidAmount;
      transactions.push({
        type: 'SALE',
        saleId: String(s._id),
        date: s.createdAt,
        reference: s.invoiceNo,
        description: s.items.map(i => `${i.nameMr || i.nameEn} x ${i.quantity}`).join(', '),
        debit: s.total,
        credit: s.paidAmount,
        netDue: s.total - s.paidAmount
      });
    });

    payments.forEach(p => {
      totalPaid += p.amount;
      transactions.push({
        type: 'PAYMENT',
        paymentId: String(p._id),
        date: p.createdAt,
        reference: `PAY-${String(p._id).slice(-6).toUpperCase()}`,
        description: `Payment Received (${p.method}) ${p.note ? '- ' + p.note : ''}`,
        debit: 0,
        credit: p.amount,
        netDue: -p.amount
      });
    });

    transactions.sort((a, b) => new Date(a.date) - new Date(b.date));

    let runningBalance = 0;
    transactions.forEach(t => {
      runningBalance += (t.debit - t.credit);
      t.runningBalance = runningBalance;
    });

    res.json({
      customer: { id: String(user._id), name: user.name, mobile: user.mobile },
      summary: { totalBilled, totalPaid, creditBalance: Math.max(0, totalBilled - totalPaid) },
      transactions
    });
  } catch (e) { next(e); }
});

// Logged-in customer ledger / statement
router.get('/me/statement', async (req, res, next) => {
  try {
    const customerId = req.auth.sub;
    const user = await User.findById(customerId);

    const [sales, payments] = await Promise.all([
      Sale.find({ customerId }).sort({ createdAt: 1 }).lean(),
      Payment.find({ customerId }).sort({ createdAt: 1 }).lean()
    ]);

    const transactions = [];
    let totalBilled = 0, totalPaid = 0;

    sales.forEach(s => {
      totalBilled += s.total;
      totalPaid += s.paidAmount;
      transactions.push({
        type: 'SALE',
        date: s.createdAt,
        reference: s.invoiceNo,
        description: s.items.map(i => `${i.nameMr || i.nameEn} x ${i.quantity}`).join(', '),
        debit: s.total,
        credit: s.paidAmount
      });
    });

    payments.forEach(p => {
      totalPaid += p.amount;
      transactions.push({
        type: 'PAYMENT',
        date: p.createdAt,
        reference: `PAY-${String(p._id).slice(-6).toUpperCase()}`,
        description: `Payment Received (${p.method}) ${p.note ? '- ' + p.note : ''}`,
        debit: 0,
        credit: p.amount
      });
    });

    transactions.sort((a, b) => new Date(a.date) - new Date(b.date));

    let runningBalance = 0;
    transactions.forEach(t => {
      runningBalance += (t.debit - t.credit);
      t.runningBalance = runningBalance;
    });

    res.json({
      customer: {
        id: String(user._id),
        name: user.name,
        mobile: user.mobile,
        address: user.address,
        profileImageUrl: user.profileImageUrl,
        createdAt: user.createdAt
      },
      summary: { totalBilled, totalPaid, creditBalance: Math.max(0, totalBilled - totalPaid), billsCount: sales.length },
      transactions
    });
  } catch (e) { next(e); }
});

router.patch('/me/profile', async (req, res, next) => {
  try {
    const customerId = req.auth.sub;
    const { name, address, profileImageUrl } = req.body || {};

    const user = await User.findById(customerId);
    if (!user) return res.status(404).json({ message: 'User not found.' });

    if (name !== undefined) user.name = name.trim();
    if (address !== undefined) user.address = address.trim();
    if (profileImageUrl !== undefined) user.profileImageUrl = profileImageUrl;

    await user.save();
    res.json({ message: 'Profile updated successfully', user: { name: user.name, mobile: user.mobile, address: user.address, profileImageUrl: user.profileImageUrl } });
  } catch (e) { next(e); }
});

// SALES & BILLING
router.post('/sales', ownerOnly, async (req, res, next) => {
  try {
    const { customerId, items, discount = 0, paymentMethod = 'cash', paidAmount, cashAmount, upiAmount, upiRefNo = '' } = req.body || {};
    if (!Array.isArray(items) || !items.length) return res.status(400).json({ message: 'At least one item is required.' });
    if (!num(discount)) return res.status(400).json({ message: 'Invalid discount.' });
    if (customerId && !await User.exists({ _id: customerId, role: 'customer', active: true })) {
      return res.status(400).json({ message: 'Invalid customer.' });
    }

    const built = [];
    let subtotal = 0, cost = 0;

    for (const it of items) {
      if (!mongoose.isValidObjectId(it.productId) || !Number.isFinite(Number(it.quantity)) || Number(it.quantity) <= 0) {
        return res.status(400).json({ message: 'Invalid product or quantity.' });
      }
      const p = await Product.findById(it.productId);
      if (!p || !p.active) return res.status(400).json({ message: 'Product unavailable.' });
      if (p.stock < Number(it.quantity)) return res.status(409).json({ message: `Insufficient stock for ${p.nameEn}.` });

      const q = Number(it.quantity);
      const line = q * p.sellingPrice;
      const co = q * p.purchasePrice;

      built.push({
        productId: p._id,
        nameMr: p.nameMr,
        nameEn: p.nameEn,
        unit: p.unit,
        quantity: q,
        unitPrice: p.sellingPrice,
        unitCost: p.purchasePrice,
        lineTotal: line,
        costTotal: co
      });
      subtotal += line;
      cost += co;
    }

    const total = Math.max(0, subtotal - Number(discount));

    let cash = Math.max(0, Number(cashAmount || 0));
    let upi = Math.max(0, Number(upiAmount || 0));
    let paid = 0;
    let method = paymentMethod;

    if (cashAmount !== undefined || upiAmount !== undefined) {
      paid = cash + upi;
      if (cash > 0 && upi > 0) method = 'split';
      else if (upi > 0) method = 'upi';
      else if (cash > 0) method = 'cash';
      else if (paid === 0) method = 'credit';
    } else {
      paid = paidAmount === undefined ? (paymentMethod === 'credit' ? 0 : total) : Number(paidAmount);
      if (paymentMethod === 'upi') { upi = paid; cash = 0; }
      else if (paymentMethod === 'cash') { cash = paid; upi = 0; }
      else if (paymentMethod === 'split') { cash = paid / 2; upi = paid / 2; }
      else if (paymentMethod === 'credit') { cash = 0; upi = 0; }
    }

    if (!num(paid)) {
      return res.status(400).json({ message: 'Invalid paid amount.' });
    }

    // Atomically decrement stock
    for (const item of built) {
      const result = await Product.updateOne({ _id: item.productId, stock: { $gte: item.quantity } }, { $inc: { stock: -item.quantity } });
      if (result.modifiedCount !== 1) {
        return res.status(409).json({ message: 'Stock changed while saving; refresh and try again.' });
      }
    }

    const invoiceNo = 'ST-' + Date.now().toString(36).toUpperCase();
    const sale = await Sale.create({
      invoiceNo,
      customerId: customerId || null,
      items: built,
      subtotal,
      discount: Number(discount),
      total,
      costOfGoods: cost,
      cashAmount: cash,
      upiAmount: upi,
      upiRefNo: String(upiRefNo || '').trim(),
      paidAmount: paid,
      paymentMethod: method,
      createdBy: req.auth.sub
    });

    res.status(201).json({ sale });
  } catch (e) { next(e); }
});

router.get('/sales', ownerOnly, async (req, res, next) => {
  try {
    const filter = {};
    if (req.query.customerId && mongoose.isValidObjectId(req.query.customerId)) {
      filter.customerId = req.query.customerId;
    }

    const now = new Date();
    if (req.query.period === 'today') {
      const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
      filter.createdAt = { $gte: todayStart };
    } else if (req.query.period === 'week') {
      const weekStart = new Date(now.getTime() - 7 * 24 * 3600 * 1000);
      filter.createdAt = { $gte: weekStart };
    } else if (req.query.period === 'month') {
      const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
      filter.createdAt = { $gte: monthStart };
    } else if (req.query.period === 'year') {
      const yearStart = new Date(now.getFullYear(), 0, 1);
      filter.createdAt = { $gte: yearStart };
    }

    if (req.query.from || req.query.to) {
      filter.createdAt = filter.createdAt || {};
      if (req.query.from) filter.createdAt.$gte = new Date(req.query.from);
      if (req.query.to) filter.createdAt.$lte = new Date(req.query.to + 'T23:59:59.999Z');
    }

    let sales = await Sale.find(filter).populate('customerId', 'name mobile').sort({ createdAt: -1 }).lean();

    if (req.query.productId) {
      sales = sales.filter(s => s.items.some(i => String(i.productId) === req.query.productId));
    }

    if (req.query.status) {
      const st = req.query.status.toLowerCase();
      sales = sales.filter(s => {
        const rem = Math.max(0, s.total - s.paidAmount);
        if (st === 'paid') return rem <= 0;
        if (st === 'unpaid') return s.paidAmount <= 0 && rem > 0;
        if (st === 'partial') return s.paidAmount > 0 && rem > 0;
        return true;
      });
    }

    res.json({ sales });
  } catch (e) { next(e); }
});

router.get('/sales/:id', async (req, res, next) => {
  try {
    if (!mongoose.isValidObjectId(req.params.id)) {
      return res.status(400).json({ message: 'Invalid sale ID format.' });
    }
    const sale = await Sale.findById(req.params.id).populate('customerId', 'name mobile').lean();
    if (!sale) return res.status(404).json({ message: 'Bill/Invoice not found.' });

    if (req.auth.role !== 'owner' && String(sale.customerId ? sale.customerId._id || sale.customerId : '') !== req.auth.sub) {
      return res.status(403).json({ message: 'Access denied.' });
    }

    res.json({ sale });
  } catch (e) { next(e); }
});

router.put('/sales/:id', ownerOnly, async (req, res, next) => {
  try {
    if (!mongoose.isValidObjectId(req.params.id)) {
      return res.status(400).json({ message: 'Invalid sale ID format.' });
    }

    const sale = await Sale.findById(req.params.id);
    if (!sale) return res.status(404).json({ message: 'Bill/Invoice not found.' });

    const { customerId, items, discount = 0, paymentMethod, paidAmount, cashAmount, upiAmount, upiRefNo, notes = '' } = req.body || {};

    if (!Array.isArray(items) || !items.length) {
      return res.status(400).json({ message: 'At least one item is required.' });
    }
    if (!num(discount) || Number(discount) < 0) {
      return res.status(400).json({ message: 'Invalid discount amount.' });
    }
    if (customerId && customerId !== '' && !await User.exists({ _id: customerId, role: 'customer', active: true })) {
      return res.status(400).json({ message: 'Invalid customer account.' });
    }

    // 1. Revert original sale item quantities back to product stock
    for (const origItem of sale.items) {
      await Product.updateOne(
        { _id: origItem.productId },
        { $inc: { stock: origItem.quantity } }
      );
    }

    // 2. Validate new items and check stock availability
    const builtItems = [];
    let subtotal = 0;
    let costOfGoods = 0;

    for (const it of items) {
      if (!mongoose.isValidObjectId(it.productId) || !Number.isFinite(Number(it.quantity)) || Number(it.quantity) <= 0) {
        for (const origItem of sale.items) {
          await Product.updateOne({ _id: origItem.productId }, { $inc: { stock: -origItem.quantity } });
        }
        return res.status(400).json({ message: 'Invalid product or quantity in line items.' });
      }

      const p = await Product.findById(it.productId);
      if (!p || !p.active) {
        for (const origItem of sale.items) {
          await Product.updateOne({ _id: origItem.productId }, { $inc: { stock: -origItem.quantity } });
        }
        return res.status(400).json({ message: `Product ${it.productId} is unavailable.` });
      }

      const q = Number(it.quantity);
      const unitPrice = (it.unitPrice !== undefined && num(it.unitPrice) && Number(it.unitPrice) >= 0) ? Number(it.unitPrice) : p.sellingPrice;
      const unitCost = p.purchasePrice;
      const lineTotal = q * unitPrice;
      const costTotal = q * unitCost;

      if (p.stock < q) {
        for (const origItem of sale.items) {
          await Product.updateOne({ _id: origItem.productId }, { $inc: { stock: -origItem.quantity } });
        }
        return res.status(409).json({ message: `Insufficient stock for ${p.nameEn}. Available: ${p.stock}` });
      }

      builtItems.push({
        productId: p._id,
        nameMr: p.nameMr,
        nameEn: p.nameEn,
        unit: p.unit,
        quantity: q,
        unitPrice,
        unitCost,
        lineTotal,
        costTotal
      });

      subtotal += lineTotal;
      costOfGoods += costTotal;
    }

    // Deduct new items from product stock
    for (const newItem of builtItems) {
      await Product.updateOne(
        { _id: newItem.productId },
        { $inc: { stock: -newItem.quantity } }
      );
    }

    // Calculate authoritative totals
    const total = Math.max(0, subtotal - Number(discount));

    let cash = sale.cashAmount || 0;
    let upi = sale.upiAmount || 0;
    let paid = sale.paidAmount;
    let method = paymentMethod || sale.paymentMethod;

    if (cashAmount !== undefined || upiAmount !== undefined) {
      cash = Math.max(0, Number(cashAmount || 0));
      upi = Math.max(0, Number(upiAmount || 0));
      paid = cash + upi;
      if (cash > 0 && upi > 0) method = 'split';
      else if (upi > 0) method = 'upi';
      else if (cash > 0) method = 'cash';
      else if (paid === 0) method = 'credit';
    } else if (paidAmount !== undefined && num(paidAmount)) {
      paid = Math.max(0, Number(paidAmount));
      if (method === 'upi') { upi = paid; cash = 0; }
      else if (method === 'cash') { cash = paid; upi = 0; }
      else if (method === 'split') { cash = paid / 2; upi = paid / 2; }
      else if (method === 'credit') { cash = 0; upi = 0; }
    }

    const prevCustomerId = sale.customerId ? String(sale.customerId) : null;
    const newCustomerId = (customerId && customerId !== '') ? String(customerId) : null;
    const prevTotal = sale.total;
    const prevPaid = sale.paidAmount;

    // 3. Update Sale fields
    sale.customerId = (customerId && customerId !== '') ? customerId : null;
    sale.items = builtItems;
    sale.subtotal = subtotal;
    sale.discount = Number(discount);
    sale.total = total;
    sale.costOfGoods = costOfGoods;
    sale.cashAmount = cash;
    sale.upiAmount = upi;
    if (upiRefNo !== undefined) sale.upiRefNo = String(upiRefNo).trim();
    sale.paidAmount = paid;
    sale.paymentMethod = method;
    if (notes) sale.notes = notes;

    // 4. Record Audit Trail
    sale.auditTrail = sale.auditTrail || [];
    sale.auditTrail.push({
      editedAt: new Date(),
      editedBy: req.auth.sub,
      previousTotal: prevTotal,
      newTotal: total,
      previousPaid: prevPaid,
      newPaid: paid,
      previousCustomerId: prevCustomerId,
      newCustomerId: newCustomerId,
      reason: notes || 'Invoice correction'
    });

    await sale.save();

    const updatedSale = await Sale.findById(sale._id).populate('customerId', 'name mobile').lean();
    res.json({ message: 'Bill corrected successfully.', sale: updatedSale });
  } catch (e) { next(e); }
});

router.patch('/sales/:id', ownerOnly, async (req, res, next) => {
  try {
    const { paidAmount, discount, paymentMethod, notes } = req.body || {};
    const sale = await Sale.findById(req.params.id);
    if (!sale) return res.status(404).json({ message: 'Bill/Invoice not found.' });

    const prevTotal = sale.total;
    const prevPaid = sale.paidAmount;

    if (paidAmount !== undefined) {
      if (!num(paidAmount) || Number(paidAmount) < 0) {
        return res.status(400).json({ message: 'Invalid paid amount.' });
      }
      sale.paidAmount = Number(paidAmount);
    }
    if (discount !== undefined && num(discount)) {
      sale.discount = Number(discount);
      sale.total = Math.max(0, sale.subtotal - sale.discount);
    }
    if (paymentMethod) sale.paymentMethod = paymentMethod;
    if (notes) sale.notes = notes;

    sale.auditTrail = sale.auditTrail || [];
    sale.auditTrail.push({
      editedAt: new Date(),
      editedBy: req.auth.sub,
      previousTotal: prevTotal,
      newTotal: sale.total,
      previousPaid: prevPaid,
      newPaid: sale.paidAmount,
      reason: notes || 'Quick payment/discount edit'
    });

    await sale.save();
    const updated = await Sale.findById(sale._id).populate('customerId', 'name mobile').lean();
    res.json({ message: 'Bill updated successfully.', sale: updated });
  } catch (e) { next(e); }
});

router.get('/me/bills', async (req, res, next) => {
  try {
    const filter = req.auth.role === 'owner' ? {} : { customerId: req.auth.sub };
    res.json({ bills: await Sale.find(filter).sort({ createdAt: -1 }).lean() });
  } catch (e) { next(e); }
});

function getLocalDateBounds(period, fromDateStr, toDateStr) {
  const now = new Date();
  const year = now.getFullYear();
  const month = now.getMonth();
  const day = now.getDate();

  let start = null;
  let end = new Date(year, month, day, 23, 59, 59, 999);

  if (period === 'today') {
    start = new Date(year, month, day, 0, 0, 0, 0);
    end = new Date(year, month, day, 23, 59, 59, 999);
  } else if (period === 'week' || period === 'this_week') {
    start = new Date(now.getTime() - 7 * 24 * 3600 * 1000);
    start.setHours(0, 0, 0, 0);
    end = new Date(year, month, day, 23, 59, 59, 999);
  } else if (period === 'month' || period === 'this_month') {
    start = new Date(year, month, 1, 0, 0, 0, 0);
    end = new Date(year, month, day, 23, 59, 59, 999);
  } else if (period === 'year' || period === 'this_year') {
    start = new Date(year, 0, 1, 0, 0, 0, 0);
    end = new Date(year, month, day, 23, 59, 59, 999);
  } else if (period === 'last_month') {
    start = new Date(year, month - 1, 1, 0, 0, 0, 0);
    end = new Date(year, month, 0, 23, 59, 59, 999);
  } else if (period === 'custom' && fromDateStr && toDateStr) {
    start = new Date(fromDateStr + 'T00:00:00.000Z');
    end = new Date(toDateStr + 'T23:59:59.999Z');
  } else {
    start = null;
    end = new Date(year + 10, 0, 1);
  }

  return { start, end };
}

router.get('/me/history', async (req, res, next) => {
  try {
    const customerId = req.auth.sub;
    const { period, startDate, endDate } = req.query || {};

    const { start, end } = getLocalDateBounds(period, startDate, endDate);
    if (period === 'custom' && startDate && endDate && new Date(endDate + 'T23:59:59.999Z') < new Date(startDate + 'T00:00:00.000Z')) {
      return res.status(400).json({ message: 'End date cannot be before start date.' });
    }

    const saleFilter = { customerId };
    const paymentFilter = { customerId };

    if (start) {
      saleFilter.createdAt = { $gte: start, $lte: end };
      paymentFilter.createdAt = { $gte: start, $lte: end };
    }

    const [periodSales, periodPayments, allSalesUpToCutoff, allPaymentsUpToCutoff, customerUser] = await Promise.all([
      Sale.find(saleFilter).populate('customerId', 'name mobile').sort({ createdAt: -1 }).lean(),
      Payment.find(paymentFilter).populate('customerId', 'name mobile').sort({ createdAt: -1 }).lean(),
      Sale.find({ customerId, createdAt: { $lte: end } }).lean(),
      Payment.find({ customerId, createdAt: { $lte: end } }).lean(),
      User.findById(customerId).select('name mobile').lean()
    ]);

    const billsCount = periodSales.length;
    const totalPurchases = periodSales.reduce((a, s) => a + (s.total || 0), 0);

    let cashPaid = 0;
    let upiPaid = 0;

    for (const s of periodSales) {
      if (s.cashAmount || s.upiAmount) {
        cashPaid += (s.cashAmount || 0);
        upiPaid += (s.upiAmount || 0);
      } else {
        if (s.paymentMethod === 'upi') upiPaid += s.paidAmount;
        else if (s.paymentMethod === 'cash') cashPaid += s.paidAmount;
        else if (s.paymentMethod === 'split') { cashPaid += s.paidAmount / 2; upiPaid += s.paidAmount / 2; }
        else cashPaid += s.paidAmount;
      }
    }

    for (const p of periodPayments) {
      if (!p.saleId) {
        if (p.cashAmount || p.upiAmount) {
          cashPaid += (p.cashAmount || 0);
          upiPaid += (p.upiAmount || 0);
        } else {
          if (p.method === 'upi') upiPaid += p.amount;
          else if (p.method === 'cash') cashPaid += p.amount;
          else if (p.method === 'split') { cashPaid += p.amount / 2; upiPaid += p.amount / 2; }
          else cashPaid += p.amount;
        }
      }
    }

    const totalPaid = cashPaid + upiPaid;

    const billedUpToCutoff = allSalesUpToCutoff.reduce((a, s) => a + (s.total || 0), 0);
    const paidOnSalesUpToCutoff = allSalesUpToCutoff.reduce((a, s) => a + (s.paidAmount || 0), 0);
    const directPaymentsUpToCutoff = allPaymentsUpToCutoff.filter(p => !p.saleId).reduce((a, p) => a + (p.amount || 0), 0);
    const endingOutstanding = billedUpToCutoff - (paidOnSalesUpToCutoff + directPaymentsUpToCutoff);

    const sanitizedSales = periodSales.map(s => {
      const { costOfGoods, auditTrail, ...cleanSale } = s;
      if (cleanSale.items) {
        cleanSale.items = cleanSale.items.map(it => {
          const { unitCost, costTotal, ...cleanItem } = it;
          return cleanItem;
        });
      }
      return cleanSale;
    });

    res.json({
      customer: customerUser,
      period: period || 'all',
      startDate: startDate || null,
      endDate: endDate || null,
      summary: {
        billsCount,
        totalPurchases,
        cashPaid,
        upiPaid,
        totalPaid,
        endingOutstanding
      },
      bills: sanitizedSales,
      payments: periodPayments
    });
  } catch (e) { next(e); }
});

// PURCHASES (Suppliers & Stock Intake)
router.post('/purchases', ownerOnly, async (req, res, next) => {
  try {
    const { supplierName, items, paidAmount, paymentMethod = 'cash', note = '' } = req.body || {};
    if (!supplierName || !Array.isArray(items) || !items.length) {
      return res.status(400).json({ message: 'Supplier name and items are required.' });
    }

    const built = [];
    let totalAmount = 0;

    for (const it of items) {
      if (!mongoose.isValidObjectId(it.productId) || !num(it.quantity) || Number(it.quantity) <= 0 || !num(it.unitPrice)) {
        return res.status(400).json({ message: 'Valid product, quantity and purchase unit price are required.' });
      }
      const p = await Product.findById(it.productId);
      if (!p) return res.status(400).json({ message: 'Product not found.' });

      const q = Number(it.quantity);
      const price = Number(it.unitPrice);
      const line = q * price;

      built.push({
        productId: p._id,
        nameMr: p.nameMr,
        nameEn: p.nameEn,
        unit: p.unit,
        quantity: q,
        unitPrice: price,
        lineTotal: line
      });
      totalAmount += line;
    }

    // Atomically increment product stock & update purchase price
    for (const item of built) {
      await Product.updateOne(
        { _id: item.productId },
        { $inc: { stock: item.quantity }, $set: { purchasePrice: item.unitPrice } }
      );
    }

    const paid = paidAmount === undefined ? totalAmount : Number(paidAmount);

    const purchaseNo = 'PO-' + Date.now().toString(36).toUpperCase();
    const purchase = await Purchase.create({
      purchaseNo,
      supplierName,
      items: built,
      totalAmount,
      paidAmount: paid,
      paymentMethod,
      note,
      createdBy: req.auth.sub
    });

    res.status(201).json({ purchase });
  } catch (e) { next(e); }
});

router.get('/purchases', ownerOnly, async (req, res, next) => {
  try {
    const filter = {};
    if (req.query.from || req.query.to) {
      filter.createdAt = {};
      if (req.query.from) filter.createdAt.$gte = new Date(req.query.from);
      if (req.query.to) filter.createdAt.$lte = new Date(req.query.to + 'T23:59:59.999Z');
    }
    res.json({ purchases: await Purchase.find(filter).sort({ createdAt: -1 }).lean() });
  } catch (e) { next(e); }
});

router.patch('/purchases/:id', ownerOnly, async (req, res, next) => {
  try {
    const { supplierName, paidAmount, addPaidAmount, paymentMethod, note } = req.body || {};
    const p = await Purchase.findById(req.params.id);
    if (!p) return res.status(404).json({ message: 'Purchase not found.' });

    if (supplierName !== undefined) p.supplierName = supplierName.trim();
    if (note !== undefined) p.note = note.trim();
    if (paymentMethod !== undefined) p.paymentMethod = paymentMethod;

    if (addPaidAmount !== undefined && num(addPaidAmount) && Number(addPaidAmount) > 0) {
      p.paidAmount = Math.min(p.totalAmount, p.paidAmount + Number(addPaidAmount));
    } else if (paidAmount !== undefined && num(paidAmount)) {
      p.paidAmount = Math.min(p.totalAmount, Number(paidAmount));
    }

    await p.save();
    res.json({ purchase: p });
  } catch (e) { next(e); }
});

router.delete('/purchases/:id', ownerOnly, async (req, res, next) => {
  try {
    const p = await Purchase.findByIdAndDelete(req.params.id);
    if (!p) return res.status(404).json({ message: 'Purchase not found.' });
    res.json({ message: 'Purchase deleted successfully.' });
  } catch (e) { next(e); }
});

// PAYMENTS & INVOICE ALLOCATION
router.post('/payments', ownerOnly, async (req, res, next) => {
  try {
    const { customerId, saleId, amount, cashAmount, upiAmount, upiRefNo = '', method = 'cash', note = '' } = req.body || {};
    if (!mongoose.isValidObjectId(customerId) || !await User.exists({ _id: customerId, role: 'customer' })) {
      return res.status(400).json({ message: 'Valid customer required.' });
    }

    let cash = Math.max(0, Number(cashAmount || 0));
    let upi = Math.max(0, Number(upiAmount || 0));
    let totalAmt = 0;
    let payMethod = method;

    if (cashAmount !== undefined || upiAmount !== undefined) {
      totalAmt = cash + upi;
      if (cash > 0 && upi > 0) payMethod = 'split';
      else if (upi > 0) payMethod = 'upi';
      else if (cash > 0) payMethod = 'cash';
    } else {
      totalAmt = Number(amount || 0);
      if (payMethod === 'upi') { upi = totalAmt; cash = 0; }
      else if (payMethod === 'cash') { cash = totalAmt; upi = 0; }
      else if (payMethod === 'split') { cash = totalAmt / 2; upi = totalAmt / 2; }
    }

    if (!num(totalAmt) || totalAmt <= 0) {
      return res.status(400).json({ message: 'Payment amount must be greater than 0.' });
    }

    let targetSale = null;
    if (saleId && mongoose.isValidObjectId(saleId)) {
      targetSale = await Sale.findOne({ _id: saleId, customerId });
      if (!targetSale) return res.status(404).json({ message: 'Target invoice not found for this customer.' });

      targetSale.cashAmount = (targetSale.cashAmount || 0) + cash;
      targetSale.upiAmount = (targetSale.upiAmount || 0) + upi;
      if (upiRefNo && !targetSale.upiRefNo) targetSale.upiRefNo = String(upiRefNo).trim();

      targetSale.paidAmount = Math.min(targetSale.total, targetSale.paidAmount + totalAmt);
      if (targetSale.cashAmount > 0 && targetSale.upiAmount > 0) targetSale.paymentMethod = 'split';
      else if (targetSale.upiAmount > 0) targetSale.paymentMethod = 'upi';
      else if (targetSale.cashAmount > 0) targetSale.paymentMethod = 'cash';

      await targetSale.save();
    }

    const p = await Payment.create({
      customerId,
      saleId: targetSale ? targetSale._id : null,
      amount: totalAmt,
      cashAmount: cash,
      upiAmount: upi,
      upiRefNo: String(upiRefNo || '').trim(),
      method: payMethod,
      note,
      createdBy: req.auth.sub
    });
    res.status(201).json({ payment: p, invoice: targetSale });
  } catch (e) { next(e); }
});

router.patch('/payments/:id', ownerOnly, async (req, res, next) => {
  try {
    const { amount, cashAmount, upiAmount, upiRefNo, method, note = '', reason = 'Correction' } = req.body || {};
    const payment = await Payment.findById(req.params.id);
    if (!payment) return res.status(404).json({ message: 'Payment not found.' });

    let cash = cashAmount !== undefined ? Math.max(0, Number(cashAmount)) : payment.cashAmount || 0;
    let upi = upiAmount !== undefined ? Math.max(0, Number(upiAmount)) : payment.upiAmount || 0;
    let newAmount = payment.amount;
    let payMethod = method || payment.method;

    if (cashAmount !== undefined || upiAmount !== undefined) {
      newAmount = cash + upi;
      if (cash > 0 && upi > 0) payMethod = 'split';
      else if (upi > 0) payMethod = 'upi';
      else if (cash > 0) payMethod = 'cash';
    } else if (amount !== undefined && num(amount)) {
      newAmount = Number(amount);
      if (payMethod === 'upi') { upi = newAmount; cash = 0; }
      else if (payMethod === 'cash') { cash = newAmount; upi = 0; }
      else if (payMethod === 'split') { cash = newAmount / 2; upi = newAmount / 2; }
    }

    if (newAmount <= 0) return res.status(400).json({ message: 'Invalid payment amount.' });

    const prevAmount = payment.amount;

    payment.auditTrail = payment.auditTrail || [];
    payment.auditTrail.push({
      editedAt: new Date(),
      editedBy: req.auth.sub,
      previousAmount: prevAmount,
      newAmount: newAmount,
      reason
    });

    payment.amount = newAmount;
    payment.cashAmount = cash;
    payment.upiAmount = upi;
    if (upiRefNo !== undefined) payment.upiRefNo = String(upiRefNo).trim();
    payment.method = payMethod;
    if (note !== undefined) payment.note = note;

    await payment.save();

    if (payment.saleId) {
      const sale = await Sale.findById(payment.saleId);
      if (sale) {
        const diff = newAmount - prevAmount;
        sale.paidAmount = Math.max(0, Math.min(sale.total, sale.paidAmount + diff));
        if (cashAmount !== undefined || upiAmount !== undefined) {
          sale.cashAmount = cash;
          sale.upiAmount = upi;
          if (upiRefNo) sale.upiRefNo = String(upiRefNo).trim();
          if (cash > 0 && upi > 0) sale.paymentMethod = 'split';
          else if (upi > 0) sale.paymentMethod = 'upi';
          else if (cash > 0) sale.paymentMethod = 'cash';
        }
        await sale.save();
      }
    }

    res.json({ message: 'Payment updated successfully.', payment });
  } catch (e) { next(e); }
});

router.get('/payments', ownerOnly, async (req, res, next) => {
  try {
    const filter = {};
    if (req.query.from || req.query.to) {
      filter.createdAt = {};
      if (req.query.from) filter.createdAt.$gte = new Date(req.query.from);
      if (req.query.to) filter.createdAt.$lte = new Date(req.query.to + 'T23:59:59.999Z');
    }
    res.json({ payments: await Payment.find(filter).populate('customerId', 'name mobile').sort({ createdAt: -1 }).lean() });
  } catch (e) { next(e); }
});

router.get('/me/payments', async (req, res, next) => {
  try {
    res.json({ payments: await Payment.find({ customerId: req.auth.sub }).sort({ createdAt: -1 }).lean() });
  } catch (e) { next(e); }
});

// EXPENSES
router.post('/expenses', ownerOnly, async (req, res, next) => {
  try {
    const { title, amount, category = 'Other', expenseDate, paymentMethod = 'cash', note = '' } = req.body || {};
    if (!title || !num(amount) || Number(amount) <= 0) {
      return res.status(400).json({ message: 'Title and valid amount required.' });
    }
    const eDate = expenseDate ? new Date(expenseDate) : new Date();
    res.status(201).json({
      expense: await Expense.create({
        title,
        amount: Number(amount),
        category,
        expenseDate: eDate,
        paymentMethod,
        note,
        createdBy: req.auth.sub
      })
    });
  } catch (e) { next(e); }
});

router.patch('/expenses/:id', ownerOnly, async (req, res, next) => {
  try {
    const allowed = ['title', 'amount', 'category', 'expenseDate', 'paymentMethod', 'note'];
    const patch = {};
    for (const k of allowed) if (req.body[k] !== undefined) patch[k] = req.body[k];

    if (patch.amount !== undefined && (!num(patch.amount) || Number(patch.amount) <= 0)) {
      return res.status(400).json({ message: 'Invalid amount.' });
    }
    if (patch.expenseDate) patch.expenseDate = new Date(patch.expenseDate);

    const exp = await Expense.findByIdAndUpdate(req.params.id, patch, { new: true });
    if (!exp) return res.status(404).json({ message: 'Expense not found.' });
    res.json({ expense: exp });
  } catch (e) { next(e); }
});

router.delete('/expenses/:id', ownerOnly, async (req, res, next) => {
  try {
    const exp = await Expense.findByIdAndDelete(req.params.id);
    if (!exp) return res.status(404).json({ message: 'Expense not found.' });
    res.json({ message: 'Expense deleted successfully.' });
  } catch (e) { next(e); }
});

router.get('/expenses', ownerOnly, async (req, res, next) => {
  try {
    const filter = {};
    if (req.query.from || req.query.to) {
      filter.expenseDate = {};
      if (req.query.from) filter.expenseDate.$gte = new Date(req.query.from);
      if (req.query.to) filter.expenseDate.$lte = new Date(req.query.to + 'T23:59:59.999Z');
    }
    res.json({ expenses: await Expense.find(filter).sort({ expenseDate: -1, createdAt: -1 }).lean() });
  } catch (e) { next(e); }
});

// REPORTS & METRICS
async function reportData(from, to, period) {
  let date = {};
  const now = new Date();

  if (period === 'last_month') {
    const firstDayLastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    const lastDayLastMonth = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59, 999);
    date = { $gte: firstDayLastMonth, $lte: lastDayLastMonth };
  } else if (period === 'this_month') {
    const firstDayThisMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    date = { $gte: firstDayThisMonth };
  } else if (period === 'this_week' || period === 'week') {
    const firstDayWeek = new Date(now.getTime() - 7 * 24 * 3600 * 1000);
    date = { $gte: firstDayWeek };
  } else if (period === 'this_year' || period === 'year') {
    const firstDayYear = new Date(now.getFullYear(), 0, 1);
    date = { $gte: firstDayYear };
  } else if (period === 'today') {
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    date = { $gte: todayStart };
  } else {
    if (from) date.$gte = new Date(from);
    if (to) date.$lte = new Date(to + 'T23:59:59.999Z');
  }
  const sf = Object.keys(date).length ? { createdAt: date } : {};

  const [sales, purchases, products, expenses, payments, allSales, allPayments, customersList] = await Promise.all([
    Sale.find(sf).populate('customerId', 'name mobile').lean(),
    Purchase.find(sf).lean(),
    Product.find({ active: true }).lean(),
    Expense.find(sf).lean(),
    Payment.find(sf).populate('customerId', 'name mobile').lean(),
    Sale.find({}).lean(),
    Payment.find({}).lean(),
    User.find({ role: 'customer' }).lean()
  ]);

  const revenue = sales.reduce((a, s) => a + s.total, 0);
  const cogs = sales.reduce((a, s) => a + s.costOfGoods, 0);
  const expenseTotal = expenses.reduce((a, e) => a + e.amount, 0);
  const purchaseTotal = purchases.reduce((a, p) => a + p.totalAmount, 0);

  // Today's Sales & Today's Purchases
  const todayStart = new Date();
  todayStart.setHours(0, 0, 0, 0);
  const todaySales = allSales.filter(s => new Date(s.createdAt) >= todayStart).reduce((a, s) => a + s.total, 0);
  const todayPurchases = purchases.filter(p => new Date(p.createdAt) >= todayStart).reduce((a, p) => a + p.totalAmount, 0);

  // Total Outstanding Customer Credit Balance
  const totalBilledAll = allSales.reduce((a, s) => a + s.total, 0);
  const totalPaidOnSalesAll = allSales.reduce((a, s) => a + s.paidAmount, 0);
  const totalDirectPaymentsAll = allPayments.filter(p => !p.saleId).reduce((a, p) => a + p.amount, 0);
  const totalOutstandingCredit = Math.max(0, totalBilledAll - (totalPaidOnSalesAll + totalDirectPaymentsAll));

  const customerMap = {};

  // Process registered customers
  customersList.forEach(c => {
    customerMap[String(c._id)] = {
      id: String(c._id),
      name: c.name,
      mobile: c.mobile,
      billed: 0,
      paid: 0,
      balance: 0
    };
  });

  // Aggregate all sales
  allSales.forEach(s => {
    const rawId = s.customerId ? (s.customerId._id || s.customerId) : null;
    const cId = rawId ? String(rawId) : 'walkin';
    if (!customerMap[cId]) {
      customerMap[cId] = {
        id: cId,
        name: cId === 'walkin' ? 'Walk-in Customers (उधारी)' : 'Customer',
        mobile: 'N/A',
        billed: 0,
        paid: 0,
        balance: 0
      };
    }
    customerMap[cId].billed += s.total;
    customerMap[cId].paid += s.paidAmount;
  });

  // Aggregate all direct payments
  allPayments.forEach(p => {
    const rawId = p.customerId ? (p.customerId._id || p.customerId) : null;
    const cId = rawId ? String(rawId) : 'walkin';
    if (customerMap[cId] && !p.saleId) {
      customerMap[cId].paid += p.amount;
    }
  });

  // Calculate balance and filter balance > 0
  const customerBalances = Object.values(customerMap)
    .map(c => ({
      ...c,
      balance: Math.max(0, c.billed - c.paid)
    }))
    .filter(c => c.balance > 0);

  const prodMap = {};
  products.forEach(p => { prodMap[String(p._id)] = p; });

  const soldQty = {};
  const soldRev = {};
  for (const s of sales) {
    for (const i of s.items) {
      const k = String(i.productId);
      const q = Number(i.quantity) || 0;
      const unitPrice = Number(i.unitPrice) || (prodMap[k] ? prodMap[k].sellingPrice : 0);
      const line = Number(i.lineTotal) || (q * unitPrice);

      soldQty[k] = (soldQty[k] || 0) + q;
      soldRev[k] = (soldRev[k] || 0) + line;
    }
  }

  const stock = products.map(p => ({
    product: p.nameEn,
    productMr: p.nameMr,
    category: p.category || 'कांडी',
    unit: p.unit,
    purchasePrice: p.purchasePrice,
    sellingPrice: p.sellingPrice,
    stock: p.stock,
    stockValue: p.stock * p.purchasePrice,
    quantitySold: soldQty[String(p._id)] || 0,
    revenueGenerated: soldRev[String(p._id)] || 0,
    lowStock: p.stock <= p.lowStockAt
  }));

  const topSelling = [...stock].filter(s => s.quantitySold > 0).sort((a, b) => b.quantitySold - a.quantitySold).slice(0, 5);
  const lowStockProducts = stock.filter(s => s.lowStock);

  let cashCollected = 0;
  let upiCollected = 0;

  for (const s of sales) {
    if (s.cashAmount || s.upiAmount) {
      cashCollected += (s.cashAmount || 0);
      upiCollected += (s.upiAmount || 0);
    } else {
      if (s.paymentMethod === 'upi') {
        upiCollected += s.paidAmount;
      } else if (s.paymentMethod === 'cash') {
        cashCollected += s.paidAmount;
      } else if (s.paymentMethod === 'split') {
        cashCollected += s.paidAmount / 2;
        upiCollected += s.paidAmount / 2;
      } else {
        cashCollected += s.paidAmount;
      }
    }
  }

  for (const p of payments) {
    if (!p.saleId) {
      if (p.cashAmount || p.upiAmount) {
        cashCollected += (p.cashAmount || 0);
        upiCollected += (p.upiAmount || 0);
      } else {
        if (p.method === 'upi') {
          upiCollected += p.amount;
        } else if (p.method === 'cash') {
          cashCollected += p.amount;
        } else if (p.method === 'split') {
          cashCollected += p.amount / 2;
          upiCollected += p.amount / 2;
        } else {
          cashCollected += p.amount;
        }
      }
    }
  }

  const paymentsReceived = cashCollected + upiCollected;

  return {
    sales,
    purchases,
    products,
    expenses,
    payments,
    revenue,
    cogs,
    grossProfit: revenue - cogs,
    expenseTotal,
    purchaseTotal,
    netProfit: revenue - cogs - expenseTotal,
    todaySales,
    todayPurchases,
    cashCollected,
    upiCollected,
    paymentsReceived,
    totalOutstandingCredit,
    stock,
    customerBalances,
    topSelling,
    lowStockProducts
  };
}

router.get('/reports/summary', ownerOnly, async (req, res, next) => {
  try {
    const r = await reportData(req.query.from, req.query.to, req.query.period);
    res.json({
      period: { from: req.query.from || null, to: req.query.to || null },
      totals: {
        todaySales: r.todaySales,
        todayPurchases: r.todayPurchases,
        salesRevenue: r.revenue,
        costOfGoodsSold: r.cogs,
        grossProfit: r.grossProfit,
        expenses: r.expenseTotal,
        purchaseTotal: r.purchaseTotal,
        netProfit: r.grossProfit,
        bills: r.sales.length,
        purchasesCount: r.purchases.length,
        productsCount: r.products.length,
        totalStockUnits: r.products.reduce((a, p) => a + (p.stock || 0), 0),
        lowStockCount: r.lowStockProducts.length,
        stockValue: r.stock.reduce((a, p) => a + p.stockValue, 0),
        cashCollected: r.cashCollected,
        upiCollected: r.upiCollected,
        paymentsReceived: r.paymentsReceived,
        totalOutstandingCredit: r.totalOutstandingCredit
      },
      stock: r.stock,
      sales: r.sales,
      purchases: r.purchases,
      expenses: r.expenses,
      payments: r.payments,
      customerBalances: r.customerBalances,
      topSelling: r.topSelling,
      lowStockProducts: r.lowStockProducts
    });
  } catch (e) { next(e); }
});

// Genuine Excel Report Download (.xlsx)
router.get('/reports/excel', ownerOnly, async (req, res, next) => {
  try {
    const r = await reportData(req.query.from, req.query.to);
    const wb = XLSX.utils.book_new();

    // Sheet 1: Product Sales
    const salesRows = r.sales.flatMap(s => s.items.map(i => ({
      Invoice: s.invoiceNo,
      Date: new Date(s.createdAt).toLocaleString(),
      Product: i.nameEn,
      Product_Marathi: i.nameMr,
      Quantity: i.quantity,
      Unit: i.unit,
      Selling_Price: i.unitPrice,
      Sales: i.lineTotal,
      Cost: i.costTotal,
      Gross_Profit: i.lineTotal - i.costTotal,
      Customer_ID: s.customerId ? String(s.customerId) : 'Walk-in'
    })));
    XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(salesRows), 'Sales History');

    // Sheet 2: Supplier Purchases
    const purchaseRows = r.purchases.flatMap(p => p.items.map(i => ({
      Purchase_No: p.purchaseNo,
      Date: new Date(p.createdAt).toLocaleString(),
      Supplier: p.supplierName,
      Product: i.nameEn,
      Product_Marathi: i.nameMr,
      Quantity: i.quantity,
      Unit: i.unit,
      Purchase_Unit_Price: i.unitPrice,
      Total_Amount: i.lineTotal,
      Note: p.note
    })));
    XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(purchaseRows), 'Supplier Purchases');

    // Sheet 3: Remaining Stock
    XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(r.stock), 'Remaining Stock');

    // Sheet 4: Profit and Loss Summary
    XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet([{
      Today_Sales: r.todaySales,
      Today_Purchases: r.todayPurchases,
      Sales_Revenue: r.revenue,
      Cost_of_Goods_Sold: r.cogs,
      Gross_Profit: r.grossProfit,
      Expenses: r.expenseTotal,
      Net_Profit_Loss: r.netProfit,
      Total_Outstanding_Credit: r.totalOutstandingCredit,
      Number_of_Bills: r.sales.length
    }]), 'Profit & Loss');

    // Sheet 5: Expenses
    XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(r.expenses.map(e => ({
      Date: new Date(e.createdAt).toLocaleString(),
      Title: e.title,
      Category: e.category,
      Amount: e.amount
    }))), 'Expenses');

    // Sheet 6: Payments
    XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(r.payments.map(p => ({
      Date: new Date(p.createdAt).toLocaleString(),
      Customer_ID: String(p.customerId),
      Amount: p.amount,
      Method: p.method,
      Note: p.note
    }))), 'Payments');

    const buf = XLSX.write(wb, { type: 'buffer', bookType: 'xlsx' });
    res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    res.setHeader('Content-Disposition', 'attachment; filename="shravan-traders-report.xlsx"');
    res.send(buf);
  } catch (e) { next(e); }
});

// JSON Database Backup
router.get('/backup/export', ownerOnly, async (req, res, next) => {
  try {
    const [products, users, sales, purchases, payments, expenses] = await Promise.all([
      Product.find({}).lean(),
      User.find({}).select('-passwordHash').lean(),
      Sale.find({}).lean(),
      Purchase.find({}).lean(),
      Payment.find({}).lean(),
      Expense.find({}).lean()
    ]);
    res.setHeader('Content-Disposition', `attachment; filename="shravan-traders-backup-${new Date().toISOString().slice(0, 10)}.json"`);
    res.json({
      schemaVersion: 1,
      createdAt: new Date().toISOString(),
      shopName: 'Shravan Traders',
      products, users, sales, purchases, payments, expenses
    });
  } catch (e) { next(e); }
});

// JSON Database Restore
router.post('/backup/restore', ownerOnly, async (req, res, next) => {
  try {
    const { products, sales, purchases, payments, expenses } = req.body || {};
    if (Array.isArray(products) && products.length) {
      await Product.deleteMany({});
      await Product.insertMany(products);
    }
    if (Array.isArray(sales) && sales.length) {
      await Sale.deleteMany({});
      await Sale.insertMany(sales);
    }
    if (Array.isArray(purchases) && purchases.length) {
      await Purchase.deleteMany({});
      await Purchase.insertMany(purchases);
    }
    if (Array.isArray(payments) && payments.length) {
      await Payment.deleteMany({});
      await Payment.insertMany(payments);
    }
    if (Array.isArray(expenses) && expenses.length) {
      await Expense.deleteMany({});
      await Expense.insertMany(expenses);
    }
    res.json({ message: 'Backup restored successfully.' });
  } catch (e) { next(e); }
});

// Cloudinary signature
router.get('/cloudinary/signature', ownerOnly, (req, res) => {
  const { CLOUDINARY_CLOUD_NAME: cloud_name, CLOUDINARY_API_KEY: api_key, CLOUDINARY_API_SECRET: api_secret } = process.env;
  if (!cloud_name || !api_key || !api_secret) return res.status(503).json({ message: 'Cloudinary is not configured.' });
  const timestamp = Math.floor(Date.now() / 1000);
  const folder = 'shravan-traders/products';
  const signature = cloudinary.utils.api_sign_request({ timestamp, folder }, api_secret);
  res.json({ cloud_name, api_key, timestamp, folder, signature });
});

// Base64 Product Image Upload
router.post('/uploads', ownerOnly, async (req, res, next) => {
  try {
    const { image } = req.body || {};
    if (!image) return res.status(400).json({ message: 'Image data required.' });

    let imageUrl = '';
    const matches = image.match(/^data:image\/([A-Za-z-+\/]+);base64,(.+)$/);

    if (matches && matches.length === 3) {
      const ext = matches[1] === 'jpeg' ? 'jpg' : matches[1];
      const buffer = Buffer.from(matches[2], 'base64');

      const { CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET } = process.env;
      if (CLOUDINARY_CLOUD_NAME && CLOUDINARY_API_KEY && CLOUDINARY_API_SECRET) {
        try {
          const uploadRes = await cloudinary.uploader.upload(image, { folder: 'shravan-traders/products' });
          imageUrl = uploadRes.secure_url;
        } catch (cErr) {
          console.warn('Cloudinary upload fallback to local storage:', cErr.message);
        }
      }

      if (!imageUrl) {
        const uploadDir = path.join(__dirname, '../uploads');
        if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir, { recursive: true });
        const fileName = 'prod_' + Date.now() + '.' + ext;
        const filePath = path.join(uploadDir, fileName);
        fs.writeFileSync(filePath, buffer);
        const host = req.get('host') || '127.0.0.1:5000';
        const proto = req.protocol || 'http';
        imageUrl = `${proto}://${host}/uploads/${fileName}`;
      }

      return res.status(201).json({ url: imageUrl });
    } else {
      return res.status(400).json({ message: 'Invalid image base64 format.' });
    }
  } catch (e) { next(e); }
});

module.exports = router;
