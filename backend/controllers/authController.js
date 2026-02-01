const { User } = require('../models');
const { generateToken } = require('../middleware/authMiddleware');
const { validationResult } = require('express-validator');

// Register new user
const register = async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                errors: errors.array()
            });
        }

        const { email, password, name, age, sex, height, weight, activityLevel } = req.body;

        // Check if user already exists
        const existingUser = await User.findOne({ where: { email } });
        if (existingUser) {
            return res.status(400).json({
                success: false,
                message: 'Email already registered.'
            });
        }

        // Create new user
        const user = await User.create({
            email,
            password,
            name,
            age,
            sex,
            height,
            weight,
            activityLevel
        });

        const token = generateToken(user.id);

        res.status(201).json({
            success: true,
            message: 'Registration successful.',
            data: {
                user: user.toJSON(),
                token
            }
        });
    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({
            success: false,
            message: 'Registration failed. Please try again.'
        });
    }
};

// Login user
const login = async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                errors: errors.array()
            });
        }

        const { email, password } = req.body;

        // Find user
        const user = await User.findOne({ where: { email } });
        if (!user) {
            return res.status(401).json({
                success: false,
                message: 'Invalid email or password.'
            });
        }

        // Check if blocked
        if (user.isBlocked) {
            return res.status(403).json({
                success: false,
                message: 'Account is blocked. Contact administrator.'
            });
        }

        // Verify password
        const isMatch = await user.comparePassword(password);
        if (!isMatch) {
            return res.status(401).json({
                success: false,
                message: 'Invalid email or password.'
            });
        }

        const token = generateToken(user.id);

        res.json({
            success: true,
            message: 'Login successful.',
            data: {
                user: user.toJSON(),
                token
            }
        });
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({
            success: false,
            message: 'Login failed. Please try again.'
        });
    }
};

// Get current user profile
const getProfile = async (req, res) => {
    try {
        res.json({
            success: true,
            data: req.user.toJSON()
        });
    } catch (error) {
        console.error('Get profile error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to get profile.'
        });
    }
};

// Update user profile
const updateProfile = async (req, res) => {
    try {
        const { name, age, sex, height, weight, activityLevel } = req.body;

        await req.user.update({
            name: name || req.user.name,
            age: age !== undefined ? age : req.user.age,
            sex: sex || req.user.sex,
            height: height !== undefined ? height : req.user.height,
            weight: weight !== undefined ? weight : req.user.weight,
            activityLevel: activityLevel || req.user.activityLevel
        });

        res.json({
            success: true,
            message: 'Profile updated successfully.',
            data: req.user.toJSON()
        });
    } catch (error) {
        console.error('Update profile error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to update profile.'
        });
    }
};

// Change password
const changePassword = async (req, res) => {
    try {
        const { currentPassword, newPassword } = req.body;

        const isMatch = await req.user.comparePassword(currentPassword);
        if (!isMatch) {
            return res.status(400).json({
                success: false,
                message: 'Current password is incorrect.'
            });
        }

        req.user.password = newPassword;
        await req.user.save();

        res.json({
            success: true,
            message: 'Password changed successfully.'
        });
    } catch (error) {
        console.error('Change password error:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to change password.'
        });
    }
};

module.exports = {
    register,
    login,
    getProfile,
    updateProfile,
    changePassword
};
