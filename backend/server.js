const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const path = require('path');

const { sequelize, User } = require('./models');

// Import routes
const authRoutes = require('./routes/authRoutes');
const workoutRoutes = require('./routes/workoutRoutes');
const goalRoutes = require('./routes/goalRoutes');
const progressRoutes = require('./routes/progressRoutes');
const adminRoutes = require('./routes/adminRoutes');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(helmet({
    contentSecurityPolicy: false,
    crossOriginEmbedderPolicy: false
}));
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Serve static files from frontend
app.use(express.static(path.join(__dirname, '..', 'frontend')));

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/workouts', workoutRoutes);
app.use('/api/goals', goalRoutes);
app.use('/api/progress', progressRoutes);
app.use('/api/admin', adminRoutes);

// Health check
app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Serve frontend for all other routes (SPA)
app.get('*', (req, res) => {
    res.sendFile(path.join(__dirname, '..', 'frontend', 'index.html'));
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error('Server error:', err);
    res.status(500).json({
        success: false,
        message: 'Internal server error'
    });
});

// Initialize database and start server
const initializeApp = async () => {
    try {
        // Sync database
        await sequelize.sync({ alter: true });
        console.log('Database synchronized successfully');

        // Create default admin if not exists
        const adminExists = await User.findOne({ where: { role: 'admin' } });
        if (!adminExists) {
            await User.create({
                email: 'admin@fitness.com',
                password: 'admin123',
                name: 'Administrator',
                role: 'admin'
            });
            console.log('Default admin created: admin@fitness.com / admin123');
        }

        // Start server on all network interfaces
        const HOST = '0.0.0.0';
        app.listen(PORT, HOST, () => {
            // Get local IP address
            const os = require('os');
            const networkInterfaces = os.networkInterfaces();
            let localIP = 'localhost';
            
            for (const interfaceName in networkInterfaces) {
                for (const iface of networkInterfaces[interfaceName]) {
                    if (iface.family === 'IPv4' && !iface.internal) {
                        localIP = iface.address;
                        break;
                    }
                }
            }
            
            console.log(`\n🏋️ Fitness Tracker Server running!`);
            console.log(`\n📍 Acces local: http://localhost:${PORT}`);
            console.log(`🌐 Acces din rețea: http://${localIP}:${PORT}`);
            console.log(`📊 API disponibil la: http://${localIP}:${PORT}/api`);
            console.log(`\n👥 Alți utilizatori pot accesa aplicația la:`);
            console.log(`   http://${localIP}:${PORT}`);
            console.log(`\nCredențiale admin implicite:`);
            console.log(`   Email: admin@fitness.com`);
            console.log(`   Password: admin123\n`);
        });
    } catch (error) {
        console.error('Failed to initialize application:', error);
        process.exit(1);
    }
};

initializeApp();
