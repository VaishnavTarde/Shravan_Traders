require('dotenv').config();
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const { User, Customer, Sale, Payment } = require('./src/models');

async function cleanupDatabase() {
  try {
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('Connected to MongoDB Atlas');

    // 1. Ensure Owner Account (Vinayak Tarde, 8390529481)
    const ownerPassHash = await bcrypt.hash('Vaish@9075', 12);
    let owner = await User.findOneAndUpdate(
      { mobile: '8390529481' },
      {
        name: 'Vinayak Tarde',
        mobile: '8390529481',
        passwordHash: ownerPassHash,
        role: 'owner',
        active: true,
        language: 'mr',
        mustChangePassword: false
      },
      { upsert: true, new: true }
    );
    console.log('✓ Owner Account Preserved: Vinayak Tarde (8390529481)');

    // 2. Ensure Vaishnav Tarde Account
    const custPassHash = await bcrypt.hash('Vaishnav@123', 12);
    let vaishnav = await User.findOneAndUpdate(
      { $or: [{ mobile: '9075202323' }, { name: /Vaishnav/i }] },
      {
        name: 'Vaishnav Tarde',
        mobile: '9075202323',
        passwordHash: custPassHash,
        role: 'customer',
        active: true,
        address: 'Kendal Bk., Rahuri',
        language: 'mr',
        mustChangePassword: false
      },
      { upsert: true, new: true }
    );
    await Customer.findOneAndUpdate({ userId: vaishnav._id }, { userId: vaishnav._id }, { upsert: true });
    console.log('✓ Customer Account Preserved: Vaishnav Tarde (9075202323)');

    // 3. Ensure Vinit Jadhav Account
    let vinit = await User.findOneAndUpdate(
      { $or: [{ mobile: '9820001122' }, { name: /Vinit/i }] },
      {
        name: 'Vinit Jadhav',
        mobile: '9820001122',
        passwordHash: custPassHash,
        role: 'customer',
        active: true,
        address: 'Kendal Bk., Rahuri',
        language: 'mr',
        mustChangePassword: false
      },
      { upsert: true, new: true }
    );
    await Customer.findOneAndUpdate({ userId: vinit._id }, { userId: vinit._id }, { upsert: true });
    console.log('✓ Customer Account Preserved: Vinit Jadhav (9820001122)');

    // 4. Find all users NOT in [owner._id, vaishnav._id, vinit._id]
    const allowedUserIds = [owner._id, vaishnav._id, vinit._id];

    const toDeleteUsers = await User.find({ _id: { $nin: allowedUserIds } }).lean();
    const toDeleteUserIds = toDeleteUsers.map(u => u._id);

    if (toDeleteUserIds.length > 0) {
      console.log(`Deleting ${toDeleteUserIds.length} test users and their related data...`);

      // Delete test users and customer profile records
      await User.deleteMany({ _id: { $in: toDeleteUserIds } });
      await Customer.deleteMany({ userId: { $in: toDeleteUserIds } });

      // Delete sales and payments associated with test users
      await Sale.deleteMany({ customerId: { $in: toDeleteUserIds } });
      await Payment.deleteMany({ customerId: { $in: toDeleteUserIds } });

      console.log('✓ Successfully deleted test users and associated sales/payments!');
    } else {
      console.log('✓ No test users to delete.');
    }

    const remainingUsers = await User.find().lean();
    console.log('\n--- FINAL USERS IN MONGODB ATLAS ---');
    remainingUsers.forEach(u => console.log(` - ${u.name} | Mobile: ${u.mobile} | Role: ${u.role} | ID: ${u._id}`));

    await mongoose.disconnect();
  } catch (err) {
    console.error('Error cleaning up DB:', err);
  }
}

cleanupDatabase();
