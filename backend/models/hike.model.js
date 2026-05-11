const mongoose = require("mongoose");

const HikeSchema = new mongoose.Schema({
    durationSeconds: {
        type: Number,
        required: true,
        min: 0
    },

    distanceKm: {
        type: Number,
        required: true,
        min: 0
    },

    steps: {
        type: Number,
        default: 0,
        min: 0
    },

    calories: {
        type: Number,
        default: 0,
        min: 0
    },

    speed: {
        avgKmh: {
            type: Number,
            default: 0,
            min: 0
        },

        maxKmh: {
            type: Number,
            default: 0,
            min: 0
        }
    },

    pace: {
        avgMinPerKm: {
            type: Number,
            default: 0,
            min: 0
        },

        bestMinPerKm: {
            type: Number,
            default: 0,
            min: 0
        }
    },

    elevation: {
        gainMeters: {
            type: Number,
            default: 0
        },

        minMeters: {
            type: Number,
            default: 0
        },

        maxMeters: {
            type: Number,
            default: 0
        }
    },

    route: [
        {
            latitude: Number,
            longitude: Number,
            timestamp: Date
        }
    ],

    startedAt: {
        type: Date,
        required: true
    },

    endedAt: {
        type: Date,
        required: true
    },

    syncStatus: {
        type: String,
        enum: ["pending", "synced", "failed"],
        default: "pending"
    },

    userId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: "User",
        required: true,
        index: true
    }

}, {
    timestamps: true
});

module.exports = mongoose.model("Hike", HikeSchema);