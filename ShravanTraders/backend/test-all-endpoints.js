require('dotenv').config();
const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const http = require('http');
const bcrypt = require('bcryptjs');
const app = require('./src/app');
const { User, Product, Customer, Sale, Purchase, Payment, Expense } = require('./src/models');

const PORT = 5099;
let server;
let mongodInstance = null;
let ownerToken = '';
let customerToken = '';
let customerId = '';
let productId = '';

function request(method, path, headers = {}, body = null) {
  return new Promise((resolve, reject) => {
    const opts = {
      hostname: '127.0.0.1',
      port: PORT,
      path: path,
      method: method,
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        ...headers
      }
    };

    const req = http.request(opts, (res) => {
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => {
        let json = null;
        try { json = JSON.parse(data); } catch (e) { json = data; }
        resolve({ status: res.statusCode, headers: res.headers, body: json });
      });
    });

    req.on('error', reject);
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

async function runTests() {
  console.log('--- STARTING SHRAVAN TRADERS FULL TEST SUITE (AUTH, PASSWORD CHANGE, BILL CORRECTION & LEDGER) ---');

  try {
    await mongoose.connect(process.env.MONGODB_URI, { serverSelectionTimeoutMS: 2000, connectTimeoutMS: 2000, socketTimeoutMS: 2000 });
    console.log('✓ Connected to MongoDB Atlas');
  } catch (err) {
    console.warn('⚠️ Atlas connection failed, using MongoMemoryServer fallback for unit test harness...');
    mongodInstance = await MongoMemoryServer.create();
    await mongoose.connect(mongodInstance.getUri());
    console.log('✓ Connected to Memory Database Fallback');
  }

  await new Promise((res) => { server = app.listen(PORT, '127.0.0.1', res); });
  console.log(`✓ Express Test Server listening on http://127.0.0.1:${PORT}`);

  try {
    // 1. Health Check
    const health = await request('GET', '/api/health');
    if (health.status !== 200 || !health.body.ok) throw new Error('Health check failed');
    console.log('✓ Health check passed');

    // 2. Setup / Login Owner
    const ownerMobile = '8390529481';
    let ownerUser = await User.findOne({ mobile: ownerMobile }).select('+passwordHash');
    if (!ownerUser) {
      const setupRes = await request('POST', '/api/setup/owner', { 'x-owner-setup-key': process.env.OWNER_SETUP_KEY }, {
        name: 'Vinayak Tarde',
        mobile: ownerMobile,
        password: 'Vaish@9075'
      });
      ownerToken = setupRes.body.token;
    } else {
      ownerUser.passwordHash = await bcrypt.hash('Vaish@9075', 12);
      await ownerUser.save();
      const loginRes = await request('POST', '/api/auth/login', {}, { mobile: ownerMobile, password: 'Vaish@9075' });
      ownerToken = loginRes.body.token;
    }
    console.log('✓ Owner authenticated');

    // 3. Register Test Customer
    const testCustMobile = '98' + Math.floor(10000000 + Math.random() * 90000000);
    const regRes = await request('POST', '/api/auth/register', {}, {
      name: 'Kisanrao Shinde',
      mobile: testCustMobile,
      password: 'CustomerPass123',
      confirmPassword: 'CustomerPass123'
    });
    if (regRes.status !== 201) throw new Error('Customer registration failed: ' + JSON.stringify(regRes.body));
    customerId = regRes.body.user.id;
    customerToken = regRes.body.token;
    console.log(`✓ Test customer registered (Mobile: ${testCustMobile}, ID: ${customerId})`);

    // 4. Verify Customer Login
    const custLoginRes = await request('POST', '/api/auth/login', {}, {
      mobile: testCustMobile,
      password: 'CustomerPass123'
    });
    if (custLoginRes.status !== 200 || !custLoginRes.body.token) {
      throw new Error('Customer login with valid credentials failed: ' + JSON.stringify(custLoginRes.body));
    }
    console.log('✓ Customer login with valid credentials succeeded');

    // 5. Verify Invalid Customer Credentials Rejected
    const custBadLogin = await request('POST', '/api/auth/login', {}, {
      mobile: testCustMobile,
      password: 'WrongPassword999'
    });
    if (custBadLogin.status !== 401) {
      throw new Error('Customer login with invalid password should be rejected with 401');
    }
    console.log('✓ Rejection of invalid customer credentials verified');

    // 6. Test Customer Change Password
    const changePassRes = await request('POST', '/api/auth/change-password', { 'Authorization': `Bearer ${customerToken}` }, {
      currentPassword: 'CustomerPass123',
      newPassword: 'NewCustomerPass456',
      confirmPassword: 'NewCustomerPass456'
    });
    if (changePassRes.status !== 200) {
      throw new Error('Customer change password failed: ' + JSON.stringify(changePassRes.body));
    }
    console.log('✓ Customer changed password successfully');

    // 7. Verify Customer Login with Old Password Fails
    const custOldPassLogin = await request('POST', '/api/auth/login', {}, {
      mobile: testCustMobile,
      password: 'CustomerPass123'
    });
    if (custOldPassLogin.status !== 401) {
      throw new Error('Customer login with old password should fail');
    }
    console.log('✓ Old customer password correctly rejected after password change');

    // 8. Verify Customer Login with New Password Succeeds
    const custNewPassLogin = await request('POST', '/api/auth/login', {}, {
      mobile: testCustMobile,
      password: 'NewCustomerPass456'
    });
    if (custNewPassLogin.status !== 200) {
      throw new Error('Customer login with new password failed');
    }
    console.log('✓ Customer login with new password succeeded');

    // 9. Test Owner Change Password
    const ownerChangePass = await request('POST', '/api/auth/change-password', { 'Authorization': `Bearer ${ownerToken}` }, {
      currentPassword: 'Vaish@9075',
      newPassword: 'NewOwnerPass789',
      confirmPassword: 'NewOwnerPass789'
    });
    if (ownerChangePass.status !== 200) {
      throw new Error('Owner change password failed: ' + JSON.stringify(ownerChangePass.body));
    }
    console.log('✓ Owner changed password successfully');

    // 10. Verify Owner Login with New Password
    const ownerNewPassLogin = await request('POST', '/api/auth/login', {}, {
      mobile: ownerMobile,
      password: 'NewOwnerPass789'
    });
    if (ownerNewPassLogin.status !== 200) {
      throw new Error('Owner login with new password failed');
    }
    const newOwnerToken = ownerNewPassLogin.body.token;
    console.log('✓ Owner login with new password succeeded');

    // Restore Owner Password to default Vaish@9075
    await request('POST', '/api/auth/change-password', { 'Authorization': `Bearer ${newOwnerToken}` }, {
      currentPassword: 'NewOwnerPass789',
      newPassword: 'Vaish@9075',
      confirmPassword: 'Vaish@9075'
    });
    console.log('✓ Owner password restored to default');

    // 11. Create Test Product
    const prodRes = await request('POST', '/api/products', { 'Authorization': `Bearer ${ownerToken}` }, {
      nameMr: 'कांडी पशुआहार',
      nameEn: 'Kandi Feed Pellets',
      category: 'कांडी',
      unit: 'bag',
      purchasePrice: 1200,
      sellingPrice: 1500,
      stock: 100,
      lowStockAt: 5
    });
    if (prodRes.status !== 201) throw new Error('Product creation failed: ' + JSON.stringify(prodRes.body));
    productId = prodRes.body.product._id;
    console.log('✓ Test product created (Selling Price: ₹1,500)');

    // 12. Create First Sale
    const sale1Res = await request('POST', '/api/sales', { 'Authorization': `Bearer ${ownerToken}` }, {
      customerId: customerId,
      items: [{ productId: productId, quantity: 2 }], // 2 x 1500 = 3000
      discount: 0,
      paymentMethod: 'credit',
      paidAmount: 1500
    });
    if (sale1Res.status !== 201) throw new Error('First sale failed: ' + JSON.stringify(sale1Res.body));
    const saleId = sale1Res.body.sale._id;
    console.log(`✓ Sale Invoice created: ${sale1Res.body.sale.invoiceNo} (Total: ₹3,000, Paid: ₹1,500)`);

    // Verify initial customer ledger
    let ledgerRes = await request('GET', '/api/customers/ledger', { 'Authorization': `Bearer ${ownerToken}` });
    let custEntry = ledgerRes.body.ledger.find(c => c.id === customerId);
    if (custEntry.creditBalance !== 1500) throw new Error('Initial outstanding balance incorrect! Expected 1500, got ' + custEntry.creditBalance);
    console.log(`✓ Initial Customer Outstanding Balance verified: ₹${custEntry.creditBalance}`);

    // 13. Test Full Bill Correction (PUT /api/sales/:id) - Change quantity from 2 to 3 (3 x 1500 = 4500)
    console.log('\n--- TESTING FULL BILL CORRECTION & AUTOMATIC LEDGER SYNC ---');
    const editSaleRes = await request('PUT', `/api/sales/${saleId}`, { 'Authorization': `Bearer ${ownerToken}` }, {
      customerId: customerId,
      items: [{ productId: productId, quantity: 3, unitPrice: 1500 }], // 3 x 1500 = 4500
      discount: 0,
      paymentMethod: 'credit',
      paidAmount: 1500,
      notes: 'Corrected quantity from 2 to 3 bags'
    });
    if (editSaleRes.status !== 200 || editSaleRes.body.sale.total !== 4500) {
      throw new Error('Bill correction PUT request failed: ' + JSON.stringify(editSaleRes.body));
    }
    console.log(`✓ Bill #${sale1Res.body.sale.invoiceNo} corrected. New Total: ₹${editSaleRes.body.sale.total}, Paid: ₹${editSaleRes.body.sale.paidAmount}`);

    // 14. Verify Customer Ledger Automatically Recalculated after Bill Edit
    ledgerRes = await request('GET', '/api/customers/ledger', { 'Authorization': `Bearer ${ownerToken}` });
    custEntry = ledgerRes.body.ledger.find(c => c.id === customerId);
    console.log(`✓ Customer Ledger after Bill Correction: Billed = ₹${custEntry.totalBilled}, Paid = ₹${custEntry.totalPaid}, Remaining Due = ₹${custEntry.creditBalance}`);
    if (custEntry.creditBalance !== 3000) {
      throw new Error('Customer ledger balance not updated correctly after bill edit! Expected 3000, got ' + custEntry.creditBalance);
    }

    // 15. Verify Customer CANNOT Edit Bills (HTTP 403 Access Denied)
    const custEditAttempt = await request('PUT', `/api/sales/${saleId}`, { 'Authorization': `Bearer ${customerToken}` }, {
      customerId: customerId,
      items: [{ productId: productId, quantity: 1 }],
      discount: 0,
      paidAmount: 0
    });
    if (custEditAttempt.status !== 403 && custEditAttempt.status !== 401) {
      throw new Error('Customer bill edit attempt should be rejected with 403 Forbidden');
    }
    console.log('✓ Customer prohibited from editing official bills (HTTP 403 verified)');

    // 16. Verify Audit Trail Recorded in Sale Document
    const getSaleRes = await request('GET', `/api/sales/${saleId}`, { 'Authorization': `Bearer ${ownerToken}` });
    if (!getSaleRes.body.sale.auditTrail || getSaleRes.body.sale.auditTrail.length === 0) {
      throw new Error('Audit trail missing in corrected sale document');
    }
    console.log(`✓ Audit Trail verified: ${getSaleRes.body.sale.auditTrail.length} edit entry recorded with reason: "${getSaleRes.body.sale.auditTrail[0].reason}"`);

    // 17. Test Product Deletion (DELETE /api/products/:id)
    console.log('\n--- TESTING PRODUCT DELETION & RESTORATION ---');
    const delProdRes = await request('DELETE', `/api/products/${productId}`, { 'Authorization': `Bearer ${ownerToken}` });
    if (delProdRes.status !== 200) throw new Error('Product deletion failed: ' + JSON.stringify(delProdRes.body));
    console.log('✓ Product deleted from active catalog');

    // 18. Verify Product Excluded from Active GET /api/products for both Owner and Customer
    const activeProdsOwner = await request('GET', '/api/products', { 'Authorization': `Bearer ${ownerToken}` });
    const activeProdsCust = await request('GET', '/api/products', { 'Authorization': `Bearer ${customerToken}` });
    if (activeProdsOwner.body.products.some(p => p._id === productId) || activeProdsCust.body.products.some(p => p._id === productId)) {
      throw new Error('Deleted product should not appear in active product list for owner or customer');
    }
    console.log('✓ Deleted product verified hidden from active catalog for all users');

    // 19. Fetch Archived / Deleted Products (GET /api/products/archived)
    const archivedRes = await request('GET', '/api/products/archived', { 'Authorization': `Bearer ${ownerToken}` });
    if (archivedRes.status !== 200 || !archivedRes.body.products.some(p => p._id === productId)) {
      throw new Error('Deleted product missing from archived products list');
    }
    console.log('✓ Deleted product found in Owner Archived Products list');

    // 20. Restore Product (POST /api/products/:id/restore)
    const restoreRes = await request('POST', `/api/products/${productId}/restore`, { 'Authorization': `Bearer ${ownerToken}` });
    if (restoreRes.status !== 200) throw new Error('Product restore failed: ' + JSON.stringify(restoreRes.body));

    // Verify product is active again
    const activeAfterRestore = await request('GET', '/api/products', { 'Authorization': `Bearer ${ownerToken}` });
    if (!activeAfterRestore.body.products.some(p => p._id === productId)) {
      throw new Error('Restored product failed to appear back in active catalog');
    }
    console.log('✓ Product successfully restored back to active catalog for all users');

    // 21. Test Mixed Payment Creation (Cash + UPI) on Bill (Example A: Total ₹3000, Cash ₹1500, UPI ₹1000 = ₹2500, Due ₹500)
    console.log('\n--- TESTING MULTIPLE PAYMENT METHODS FOR ONE BILL (CASH + UPI) ---');
    const mixedSaleRes = await request('POST', '/api/sales', { 'Authorization': `Bearer ${ownerToken}` }, {
      customerId: customerId,
      items: [{ productId: productId, quantity: 2 }], // 2 x 1500 = 3000
      discount: 0,
      cashAmount: 1500,
      upiAmount: 1000,
      upiRefNo: 'UPI-REF-987654',
      paymentMethod: 'split'
    });

    if (mixedSaleRes.status !== 201 || mixedSaleRes.body.sale.paymentMethod !== 'split') {
      throw new Error('Mixed payment sale creation failed: ' + JSON.stringify(mixedSaleRes.body));
    }
    const mixedSale = mixedSaleRes.body.sale;
    console.log(`✓ Mixed Bill #${mixedSale.invoiceNo} created: Total ₹${mixedSale.total}, Cash ₹${mixedSale.cashAmount}, UPI ₹${mixedSale.upiAmount} (Ref: ${mixedSale.upiRefNo}), Paid ₹${mixedSale.paidAmount}`);

    if (mixedSale.cashAmount !== 1500 || mixedSale.upiAmount !== 1000 || mixedSale.paidAmount !== 2500) {
      throw new Error('Mixed payment components incorrect! Expected Cash 1500, UPI 1000, Total Paid 2500');
    }

    // 22. Record Mixed Payment on Customer Account (POST /api/payments)
    const mixedPayRes = await request('POST', '/api/payments', { 'Authorization': `Bearer ${ownerToken}` }, {
      customerId: customerId,
      saleId: mixedSale._id,
      cashAmount: 300,
      upiAmount: 200,
      upiRefNo: 'UPI-PAY-112233',
      method: 'split',
      note: 'Remaining payment'
    });

    if (mixedPayRes.status !== 201 || mixedPayRes.body.payment.method !== 'split') {
      throw new Error('Mixed payment recording failed: ' + JSON.stringify(mixedPayRes.body));
    }
    console.log(`✓ Mixed Payment recorded against Bill #${mixedSale.invoiceNo}: Cash ₹${mixedPayRes.body.payment.cashAmount}, UPI ₹${mixedPayRes.body.payment.upiAmount}`);

    // 23. Test Editing Mixed Payment Bill (PUT /api/sales/:id) - Change Cash to 1800 & UPI to 1200
    const editMixedRes = await request('PUT', `/api/sales/${mixedSale._id}`, { 'Authorization': `Bearer ${ownerToken}` }, {
      customerId: customerId,
      items: [{ productId: productId, quantity: 2 }],
      discount: 0,
      cashAmount: 1800,
      upiAmount: 1200,
      upiRefNo: 'UPI-REF-EDITED',
      paymentMethod: 'split',
      notes: 'Corrected mixed payment components'
    });

    if (editMixedRes.status !== 200 || editMixedRes.body.sale.cashAmount !== 1800 || editMixedRes.body.sale.upiAmount !== 1200) {
      throw new Error('Editing mixed payment bill failed: ' + JSON.stringify(editMixedRes.body));
    }
    console.log(`✓ Mixed Bill corrected: Cash ₹${editMixedRes.body.sale.cashAmount}, UPI ₹${editMixedRes.body.sale.upiAmount}, Total Paid ₹${editMixedRes.body.sale.paidAmount}`);

    // 24. Verify Reports Summary (Cash Collected vs UPI Collected vs Total Received)
    const reportRes = await request('GET', '/api/reports/summary', { 'Authorization': `Bearer ${ownerToken}` });
    const rTotals = reportRes.body.totals;
    console.log(`✓ Financial Report Collections Verified: Cash Collected = ₹${rTotals.cashCollected}, UPI Collected = ₹${rTotals.upiCollected}, Total Received = ₹${rTotals.paymentsReceived}`);
    if (rTotals.paymentsReceived !== (rTotals.cashCollected + rTotals.upiCollected)) {
      throw new Error('Total received does not equal Cash Collected + UPI Collected!');
    }

    // 25. Test Customer History API (GET /api/me/history) with Date Filters
    console.log('\n--- TESTING CUSTOMER PURCHASE HISTORY & DATE FILTERS ---');
    const custHistToday = await request('GET', '/api/me/history?period=today', { 'Authorization': `Bearer ${customerToken}` });
    if (custHistToday.status !== 200 || !custHistToday.body.summary) {
      throw new Error('Customer history GET request failed: ' + JSON.stringify(custHistToday.body));
    }
    const cSum = custHistToday.body.summary;
    console.log(`✓ Customer History Today Filter: Bills Count = ${cSum.billsCount}, Total Purchases = ₹${cSum.totalPurchases}, Cash Paid = ₹${cSum.cashPaid}, UPI Paid = ₹${cSum.upiPaid}, Ending Due = ₹${cSum.endingOutstanding}`);

    // 26. Test Custom Date Range Filter (GET /api/me/history?period=custom)
    const custHistCustom = await request('GET', '/api/me/history?period=custom&startDate=2026-01-01&endDate=2026-12-31', { 'Authorization': `Bearer ${customerToken}` });
    if (custHistCustom.status !== 200 || custHistCustom.body.period !== 'custom') {
      throw new Error('Custom date range filter failed');
    }
    console.log(`✓ Custom Date Range Filter (2026-01-01 to 2026-12-31): Bills = ${custHistCustom.body.bills.length}, Payments = ${custHistCustom.body.payments.length}`);

    // 27. Test Invalid Date Range Error Handling (endDate before startDate)
    const invalidDateRes = await request('GET', '/api/me/history?period=custom&startDate=2026-12-31&endDate=2026-01-01', { 'Authorization': `Bearer ${customerToken}` });
    if (invalidDateRes.status !== 400) {
      throw new Error('Invalid date range should return 400 Bad Request');
    }
    console.log('✓ Invalid custom date range correctly rejected with HTTP 400');

    // 28. Verify Customer Privacy & Data Isolation (costOfGoods omitted from customer response)
    if (custHistToday.body.bills.length > 0 && custHistToday.body.bills[0].costOfGoods !== undefined) {
      throw new Error('Customer history API must not expose internal costOfGoods to customer!');
    }
    console.log('✓ Customer Privacy verified (internal cost fields omitted from customer history response)');

    // 29. Test Forgot Password & Verification Code Generation
    console.log('\n--- TESTING FORGOT PASSWORD & RECOVERY FLOW ---');
    const forgotRes = await request('POST', '/api/auth/forgot-password', {}, { identifier: testCustMobile });
    if (forgotRes.status !== 200 || !forgotRes.body.devCode) {
      throw new Error('Forgot password request failed: ' + JSON.stringify(forgotRes.body));
    }
    const devCode = forgotRes.body.devCode;
    console.log(`✓ Forgot Password OTP code generated for ${testCustMobile}: ${devCode}`);

    // 30. Test Verify Reset Code
    const verifyCodeRes = await request('POST', '/api/auth/verify-reset-code', {}, { identifier: testCustMobile, code: devCode });
    if (verifyCodeRes.status !== 200 || !verifyCodeRes.body.resetToken) {
      throw new Error('Verify reset code failed: ' + JSON.stringify(verifyCodeRes.body));
    }
    const resetToken = verifyCodeRes.body.resetToken;
    console.log('✓ Reset code verified and single-use resetToken generated');

    // 31. Test Reset Password with Token
    const resetPassRes = await request('POST', '/api/auth/reset-password', {}, { identifier: testCustMobile, resetToken: resetToken, newPassword: 'ResetPassword@123' });
    if (resetPassRes.status !== 200) {
      throw new Error('Reset password failed: ' + JSON.stringify(resetPassRes.body));
    }
    console.log('✓ Password reset successfully with new password');

    // Verify login with new password
    const loginNewRes = await request('POST', '/api/auth/login', {}, { mobile: testCustMobile, password: 'ResetPassword@123' });
    if (loginNewRes.status !== 200) {
      throw new Error('Login with new reset password failed!');
    }
    console.log('✓ Customer login with reset password verified');

    // 32. Test Google Sign-In Endpoint (POST /api/auth/google)
    console.log('\n--- TESTING GOOGLE SIGN-IN ---');
    const googleRes = await request('POST', '/api/auth/google', {}, { idToken: 'test_google_token_' + Date.now() });
    if (googleRes.status !== 200 || !googleRes.body.token) {
      throw new Error('Google sign-in endpoint failed: ' + JSON.stringify(googleRes.body));
    }
    console.log(`✓ Google Sign-In verified for customer: ${googleRes.body.user.name} (Role: ${googleRes.body.user.role})`);

    console.log('\n========================================================================');
    console.log('🎉 ALL AUTOMATED TESTS PASSED (AUTH, PASSWORDS, BILL EDIT, DELETION, RESTORE, MIXED PAYMENTS, HISTORY & GOOGLE SIGN-IN)! 🎉');
    console.log('========================================================================\n');

  } finally {
    if (server) server.close();
    if (mongoose.connection.readyState !== 0) await mongoose.disconnect();
    if (mongodInstance) await mongodInstance.stop();
    console.log('Test Server closed.');
  }
}

runTests().catch((err) => {
  console.error('\n❌ AUTOMATED TEST ERROR:', err);
  if (server) server.close();
  mongoose.disconnect();
  if (mongodInstance) mongodInstance.stop();
  process.exit(1);
});
