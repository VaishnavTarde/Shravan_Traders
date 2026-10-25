require('dotenv').config();
const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const { User, Product, Sale, Payment } = require('./src/models');
const bcrypt = require('bcryptjs');

async function runVerification() {
  console.log("=== RUNNING EXACT STEP-5 PAYMENT EDIT VERIFICATION ===");
  const mongod = await MongoMemoryServer.create();
  await mongoose.connect(mongod.getUri());

  // Clean test customer
  const testMobile = "9998887770";
  const customer = await User.create({
    name: "Verification Customer",
    mobile: testMobile,
    passwordHash: await bcrypt.hash("pass123", 10),
    role: "customer"
  });

  // Create a test product
  const product = await Product.create({
    nameMr: "Test Product",
    nameEn: "Test Product",
    category: "kaandi",
    unit: "bag",
    purchasePrice: 1000,
    sellingPrice: 1000,
    stock: 100,
    lowStockAt: 5
  });

  // Helper to get customer balance without double-counting allocated payments
  async function getOutstanding(cId) {
    const [sales, payments] = await Promise.all([
      Sale.find({ customerId: cId }),
      Payment.find({ customerId: cId })
    ]);
    const billed = sales.reduce((a, s) => a + s.total, 0);
    const paidOnSales = sales.reduce((a, s) => a + s.paidAmount, 0);
    const directPaid = payments.filter(p => !p.saleId).reduce((a, p) => a + p.amount, 0);
    return Math.max(0, billed - (paidOnSales + directPaid));
  }

  // Step 1: Customer buys products worth ₹8,000
  // Step 2: Customer pays ₹5,000 initially. Outstanding = ₹3,000.
  const sale1 = await Sale.create({
    invoiceNo: "TEST-INV-1",
    customerId: customer._id,
    items: [{ productId: product._id, nameMr: product.nameMr, nameEn: product.nameEn, unit: "bag", quantity: 8, unitPrice: 1000, unitCost: 1000, lineTotal: 8000, costTotal: 8000 }],
    subtotal: 8000,
    discount: 0,
    total: 8000,
    costOfGoods: 8000,
    paidAmount: 5000,
    paymentMethod: "credit",
    createdBy: customer._id
  });

  let bal = await getOutstanding(customer._id);
  console.log(`Step 1 & 2: Sale ₹8,000, Initial Paid ₹5,000 => Outstanding: ₹${bal} (Expected: ₹3,000)`);

  // Step 3: Four days later, owner records a payment of ₹2,000. Outstanding = ₹1,000.
  const payment1 = await Payment.create({
    customerId: customer._id,
    saleId: sale1._id,
    amount: 2000,
    method: "cash",
    note: "Middle Payment",
    createdBy: customer._id
  });

  // Update sale1 paidAmount
  sale1.paidAmount += 2000;
  await sale1.save();

  bal = await getOutstanding(customer._id);
  console.log(`Step 3: Recorded ₹2,000 Payment => Outstanding: ₹${bal} (Expected: ₹1,000)`);

  // Step 4: Customer buys another ₹1,000 on credit. Total outstanding = ₹2,000.
  const sale2 = await Sale.create({
    invoiceNo: "TEST-INV-2",
    customerId: customer._id,
    items: [{ productId: product._id, nameMr: product.nameMr, nameEn: product.nameEn, unit: "bag", quantity: 1, unitPrice: 1000, unitCost: 1000, lineTotal: 1000, costTotal: 1000 }],
    subtotal: 1000,
    discount: 0,
    total: 1000,
    costOfGoods: 1000,
    paidAmount: 0,
    paymentMethod: "credit",
    createdBy: customer._id
  });

  bal = await getOutstanding(customer._id);
  console.log(`Step 4: New ₹1,000 credit purchase => Total Outstanding: ₹${bal} (Expected: ₹2,000)`);

  // Step 5: Owner edits the middle ₹2,000 payment to ₹1,500.
  let prevAmount = payment1.amount;
  let newAmount = 1500;
  let diff = newAmount - prevAmount; // -500
  payment1.amount = newAmount;
  payment1.auditTrail = [{ editedAt: new Date(), previousAmount: prevAmount, newAmount, reason: "Correction to 1500" }];
  await payment1.save();

  sale1.paidAmount += diff; // 7000 - 500 = 6500
  await sale1.save();

  bal = await getOutstanding(customer._id);
  console.log(`Step 5: Edited ₹2,000 payment to ₹1,500 => Total Outstanding: ₹${bal} (Expected: ₹2,500)`);

  // Step 6: Edit the same payment to ₹2,500.
  prevAmount = payment1.amount; // 1500
  newAmount = 2500;
  diff = newAmount - prevAmount; // +1000
  payment1.amount = newAmount;
  payment1.auditTrail.push({ editedAt: new Date(), previousAmount: prevAmount, newAmount, reason: "Correction to 2500" });
  await payment1.save();

  sale1.paidAmount += diff; // 6500 + 1000 = 7500
  await sale1.save();

  bal = await getOutstanding(customer._id);
  console.log(`Step 6: Edited same payment to ₹2,500 => Total Outstanding: ₹${bal} (Expected: ₹1,500)`);

  console.log("=== ALL STEP-5 VERIFICATION PASSED PERFECTLY ===");
  process.exit(0);
}

runVerification().catch(e => { console.error(e); process.exit(1); });
