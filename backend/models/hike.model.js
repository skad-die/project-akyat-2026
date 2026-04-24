const mongoose = require("mongoose");

const HikeSchema = new mongoose.Schema({
    duration: {
        type: String, // "hh:mm:ss" TODO change to proper datatype
        required: true
    },

    distanceKm: {
        type: Number,
        required: true,
        min: 0
    },

    calories: {
        type: Number,
        min: 0,
        default: 0
    },

    // km/h
    speed: {
    avg: { type: Number, min: 0 },
    max: { type: Number, min: 0 }
    },

    // min/km
    pace: {
        avg: { type: Number, min: 0 },
        max: { type: Number, min: 0 }
    },

    elevation: {
        min: Number,
        max: Number
    },

    userId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: "User",
        required: true
    }

}, {
  timestamps: true
});

module.exports = mongoose.model("Hike", HikeSchema);