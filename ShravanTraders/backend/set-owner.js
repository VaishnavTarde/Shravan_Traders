require('dotenv').config();
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

async function run() {
  await mongoose.connect(process.env.MONGODB_URI);
  const db = mongoose.connection.db;
  const hash = await bcrypt.hash('Vaish@9075', 12);

  await db.collection('users').updateOne(
    { mobile: '8390529481' },
    { $set: { name: 'Vinayak Tarde', mobile: '8390529481', passwordHash: hash, role: 'owner', active: true, mustChangePassword: false } },
    { upsert: true }
  );

  await db.collection('users').updateOne(
    { mobile: '9876543210' },
    { $set: { passwordHash: hash, role: 'owner', active: true } }
  );

  console.log('✓ Owner account login details set in Atlas cloud DB!');
  process.exit(0);
}

run().catch(console.error);
