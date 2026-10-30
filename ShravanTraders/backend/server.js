require('dotenv').config();
const mongoose = require('mongoose');
const path = require('path');
const app = require('./src/app');
const { User, Product } = require('./src/models');
const bcrypt = require('bcryptjs');

const port = Number(process.env.PORT || 5000);

if (!process.env.MONGODB_URI) {
  console.error('Missing MONGODB_URI in backend/.env');
  process.exit(1);
}
if (!process.env.JWT_SECRET || process.env.JWT_SECRET.length < 32) {
  console.error('JWT_SECRET must be at least 32 characters');
  process.exit(1);
}

mongoose.set('strictQuery', true);

// Start Express HTTP server immediately
const server = app.listen(port, '0.0.0.0', () => {
  console.log(`Shravan Traders API listening on port ${port}`);
});

async function seedDefaultData() {
  try {
    // Seed initial products if DB is empty
    const count = await Product.countDocuments();
    if (count === 0) {
      await Product.insertMany([
        { nameMr: 'पेंड (सरकी पेंड)', nameEn: 'Cottonseed Cake (Pend)', category: 'पेंड', unit: 'bag', purchasePrice: 1400, sellingPrice: 1650, stock: 50, lowStockAt: 5 },
        { nameMr: 'कांडी पशुआहार', nameEn: 'Kandi Feed Pellets', category: 'कांडी', unit: 'bag', purchasePrice: 1200, sellingPrice: 1420, stock: 40, lowStockAt: 5 },
        { nameMr: 'वालीस पशुआहार (मका बालिस)', nameEn: 'Walis Cattle Feed', category: 'वालीस', unit: 'bag', purchasePrice: 1100, sellingPrice: 1350, stock: 30, lowStockAt: 5 },
        { nameMr: 'मक्याचे पीठ (मका चुरा)', nameEn: 'Maize Meal / Corn Flour', category: 'मक्याचे पीठ', unit: 'bag', purchasePrice: 950, sellingPrice: 1150, stock: 25, lowStockAt: 5 }
      ]);
      console.log('✓ Default cattle feed products seeded!');
    }

    // Seed default initial owner account (Vinayak Tarde, 8390529481) if no owner exists
    const ownerExists = await User.exists({ role: 'owner' });
    if (!ownerExists) {
      const passwordHash = await bcrypt.hash('Vaish@9075', 12);
      await User.create({
        name: 'Vinayak Tarde',
        mobile: '8390529481',
        passwordHash,
        role: 'owner',
        active: true,
        language: 'mr',
        mustChangePassword: false
      });
      console.log('✓ Initial owner account seeded (Vinayak Tarde, Mobile: 8390529481, Password: Vaish@9075)');
    }
  } catch (err) {
    console.error('Seeding error:', err.message);
  }
}

async function startDatabase() {
  console.log('Connecting to MongoDB Atlas...');
  try {
    await mongoose.connect(process.env.MONGODB_URI, {
      serverSelectionTimeoutMS: 8000,
      connectTimeoutMS: 10000
    });
    console.log('✓ Connected to MongoDB Atlas successfully!');
    await seedDefaultData();
  } catch (atlasErr) {
    console.error('❌ Could not connect to MongoDB Atlas:', atlasErr.message);
    console.log('Will retry connecting to MongoDB Atlas periodically in 10 seconds...');
    setTimeout(startDatabase, 10000);
  }
}

startDatabase();

// Keep-alive: Ping Render backend every 14 minutes to prevent free-tier 15-minute idle spin-down
const PING_INTERVAL = 14 * 60 * 1000;
const PING_URL = process.env.RENDER_EXTERNAL_URL
  ? `${process.env.RENDER_EXTERNAL_URL}/api/health`
  : 'https://shravan-traders-backend.onrender.com/api/health';

if (process.env.NODE_ENV === 'production' || process.env.RENDER || PING_URL.includes('onrender.com')) {
  setInterval(() => {
    try {
      const https = require('https');
      const http = require('http');
      const client = PING_URL.startsWith('https') ? https : http;
      client.get(PING_URL, (res) => {
        console.log(`[Keep-Alive] Pinged backend at ${new Date().toISOString()} - Status: ${res.statusCode}`);
      }).on('error', (err) => {
        console.warn(`[Keep-Alive] Ping note: ${err.message}`);
      });
    } catch (e) {
      console.warn(`[Keep-Alive] Exception: ${e.message}`);
    }
  }, PING_INTERVAL);
}
