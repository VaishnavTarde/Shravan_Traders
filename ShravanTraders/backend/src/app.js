const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const path = require('path');
const mongoose = require('mongoose');
const routes = require('./routes');

const app = express();
app.disable('x-powered-by');
app.use(helmet());

const origins = (process.env.CORS_ORIGIN || '*').split(',').map(s => s.trim());
app.use(cors({
  origin: (origin, cb) => {
    if (!origin || origins.includes('*') || origins.includes(origin)) return cb(null, true);
    cb(new Error('Origin not allowed'));
  }
}));

app.use(express.json({ limit: '10mb' })); // Increased limit for base64 image uploads
app.use('/uploads', express.static(path.join(__dirname, '../uploads')));

// Health check endpoint
app.get('/api/health', (req, res) => {
  const dbState = mongoose.connection.readyState;
  res.json({
    ok: dbState === 1,
    databaseConnected: dbState === 1,
    dbState: dbState === 1 ? 'connected' : (dbState === 2 ? 'connecting' : 'disconnected')
  });
});

// Database connection health middleware
app.use((req, res, next) => {
  if (req.path.startsWith('/api/') && req.path !== '/api/health' && mongoose.connection.readyState !== 1) {
    return res.status(503).json({
      message: 'Database connection error. Unable to connect to MongoDB Atlas database. Please verify network and Atlas IP access list.'
    });
  }
  next();
});

app.use('/api/auth/login', rateLimit({ windowMs: 15 * 60 * 1000, limit: 20, standardHeaders: true, legacyHeaders: false }));
app.use('/api/auth/register', rateLimit({ windowMs: 15 * 60 * 1000, limit: 10, standardHeaders: true, legacyHeaders: false }));

app.use('/api', routes);

app.use((err, req, res, next) => {
  console.error(err.message);
  if (res.headersSent) return next(err);
  res.status(500).json({ message: 'Server error. Check backend logs.' });
});

module.exports = app;
