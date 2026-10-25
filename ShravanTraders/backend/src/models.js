const mongoose = require('mongoose');
const { Schema } = mongoose;

const userSchema = new Schema({
  name: { type: String, required: true, trim: true, maxlength: 100 },
  mobile: { type: String, required: true, unique: true, trim: true, maxlength: 20 },
  email: { type: String, trim: true, lowercase: true, default: '' },
  passwordHash: { type: String, required: true, select: false },
  role: { type: String, enum: ['owner', 'customer'], required: true, default: 'customer' },
  active: { type: Boolean, default: true },
  language: { type: String, enum: ['en', 'mr'], default: 'mr' },
  mustChangePassword: { type: Boolean, default: false },
  googleId: { type: String, default: '' },
  resetTokenHash: { type: String, select: false, default: '' },
  resetTokenExpires: { type: Date, default: null },
  otpCodeHash: { type: String, select: false, default: '' },
  otpExpires: { type: Date, default: null },
  address: { type: String, default: '' },
  profileImageUrl: { type: String, default: '' }
}, { timestamps: true });

const productSchema = new Schema({
  nameMr: { type: String, required: true, trim: true },
  nameEn: { type: String, required: true, trim: true },
  category: { type: String, default: 'कांडी' },
  unit: { type: String, enum: ['kg', 'bag', 'quintal', 'piece'], default: 'bag' },
  purchasePrice: { type: Number, required: true, min: 0 },
  sellingPrice: { type: Number, required: true, min: 0 },
  stock: { type: Number, required: true, min: 0, default: 0 },
  lowStockAt: { type: Number, min: 0, default: 5 },
  imageUrl: { type: String, default: '' },
  active: { type: Boolean, default: true }
}, { timestamps: true });

const customerSchema = new Schema({
  userId: { type: Schema.Types.ObjectId, ref: 'User', required: true, unique: true },
  notes: { type: String, default: '' }
}, { timestamps: true });

const saleItemSchema = new Schema({
  productId: { type: Schema.Types.ObjectId, ref: 'Product', required: true },
  nameMr: String,
  nameEn: String,
  unit: String,
  quantity: { type: Number, required: true, min: 0.001 },
  unitPrice: { type: Number, required: true, min: 0 },
  unitCost: { type: Number, required: true, min: 0 },
  lineTotal: { type: Number, required: true, min: 0 },
  costTotal: { type: Number, required: true, min: 0 }
}, { _id: false });

const saleSchema = new Schema({
  invoiceNo: { type: String, required: true, unique: true },
  customerId: { type: Schema.Types.ObjectId, ref: 'User', default: null },
  items: { type: [saleItemSchema], required: true },
  subtotal: { type: Number, required: true },
  discount: { type: Number, default: 0, min: 0 },
  total: { type: Number, required: true },
  costOfGoods: { type: Number, required: true },
  cashAmount: { type: Number, default: 0, min: 0 },
  upiAmount: { type: Number, default: 0, min: 0 },
  upiRefNo: { type: String, default: '' },
  paidAmount: { type: Number, default: 0, min: 0 },
  paymentMethod: { type: String, enum: ['cash', 'upi', 'credit', 'split'], default: 'cash' },
  notes: { type: String, default: '' },
  auditTrail: [{
    editedAt: { type: Date, default: Date.now },
    editedBy: { type: Schema.Types.ObjectId, ref: 'User' },
    previousTotal: Number,
    newTotal: Number,
    previousPaid: Number,
    newPaid: Number,
    previousCustomerId: { type: Schema.Types.ObjectId, ref: 'User' },
    newCustomerId: { type: Schema.Types.ObjectId, ref: 'User' },
    reason: String
  }],
  createdBy: { type: Schema.Types.ObjectId, ref: 'User', required: true }
}, { timestamps: true });

const purchaseItemSchema = new Schema({
  productId: { type: Schema.Types.ObjectId, ref: 'Product', required: true },
  nameMr: String,
  nameEn: String,
  unit: String,
  quantity: { type: Number, required: true, min: 0.001 },
  unitPrice: { type: Number, required: true, min: 0 },
  lineTotal: { type: Number, required: true, min: 0 }
}, { _id: false });

const purchaseSchema = new Schema({
  purchaseNo: { type: String, required: true, unique: true },
  supplierName: { type: String, required: true, trim: true },
  items: { type: [purchaseItemSchema], required: true },
  totalAmount: { type: Number, required: true, min: 0 },
  paidAmount: { type: Number, default: 0, min: 0 },
  paymentMethod: { type: String, enum: ['cash', 'upi', 'credit'], default: 'cash' },
  note: { type: String, default: '' },
  createdBy: { type: Schema.Types.ObjectId, ref: 'User', required: true }
}, { timestamps: true });

const paymentSchema = new Schema({
  customerId: { type: Schema.Types.ObjectId, ref: 'User', required: true },
  saleId: { type: Schema.Types.ObjectId, ref: 'Sale', default: null },
  amount: { type: Number, required: true, min: 0.01 },
  cashAmount: { type: Number, default: 0, min: 0 },
  upiAmount: { type: Number, default: 0, min: 0 },
  upiRefNo: { type: String, default: '' },
  method: { type: String, enum: ['cash', 'upi', 'split', 'other'], default: 'cash' },
  note: { type: String, default: '' },
  auditTrail: [{
    editedAt: { type: Date, default: Date.now },
    editedBy: { type: Schema.Types.ObjectId, ref: 'User' },
    previousAmount: Number,
    newAmount: Number,
    reason: String
  }],
  createdBy: { type: Schema.Types.ObjectId, ref: 'User', required: true }
}, { timestamps: true });

const expenseSchema = new Schema({
  title: { type: String, required: true, trim: true },
  amount: { type: Number, required: true, min: 0.01 },
  category: { type: String, default: 'Other' },
  expenseDate: { type: Date, default: Date.now },
  paymentMethod: { type: String, default: 'cash' },
  note: { type: String, default: '' },
  createdBy: { type: Schema.Types.ObjectId, ref: 'User', required: true }
}, { timestamps: true });

module.exports = {
  User: mongoose.model('User', userSchema),
  Product: mongoose.model('Product', productSchema),
  Customer: mongoose.model('Customer', customerSchema),
  Sale: mongoose.model('Sale', saleSchema),
  Purchase: mongoose.model('Purchase', purchaseSchema),
  Payment: mongoose.model('Payment', paymentSchema),
  Expense: mongoose.model('Expense', expenseSchema)
};
