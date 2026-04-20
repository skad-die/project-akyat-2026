const mongoose = require("mongoose");

const HikeSchema = new mongoose.Schema({
  title: {
    type: String,
    required: [true, "Please provide a name for this hike"],
    trim: true,
    index: true
  },
  date: {
    type: Date,
    default: Date.now
  },
  distance: {
    type: Number,
    required: true,
    min: 0
  },
  duration: {
    type: Number,
    required: true
  },
  elevationGain: Number,
  difficulty: {
    type: String,
    enum: ['Easy', 'Moderate', 'Hard', 'Expert'],
    default: 'Moderate'
  },
  pace: {
    type: Number,
    default: 0
  },
  calories: {
    type: Number,
    default: 0
  }
}, {
  timestamps: true
});

module.exports = mongoose.model("Hike", HikeSchema);
