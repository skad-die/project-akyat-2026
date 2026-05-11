require("dotenv").config();

if (!process.env.JWT_SECRET) {
  throw new Error("JWT_SECRET is not defined");
}

const express = require("express");
const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const connectDB = require("./db");
const User = require("./models/user.model");
const Hike = require("./models/hike.model");

const app = express();
const cors = require("cors");

app.use(express.json());
app.use(cors());

connectDB();

// AUTH MIDDLEWARE
const auth = (req, res, next) => {
    const authHeader = req.header("Authorization");
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
        return res.status(401).json({ message: "No or invalid token format" });
    }

    const token = authHeader.split(" ")[1];
    try {
        req.user = jwt.verify(token, process.env.JWT_SECRET);
        next();
    } catch (err) {
        return res.status(401).json({ message: "Invalid token" });
    }
};

// TEST
app.get("/", (req, res) => {
  res.send("Hike API running");
});

// REGISTER
app.post("/register", async (req, res) => {
  try {
    const { name, email, password } = req.body;

    if (!name || !email || !password) {
      return res.status(400).json({ message: "All fields required" });
    }

    const existingUser = await User.findOne({ email });
    if (existingUser) return res.status(400).json({ message: "Email already exists" });

    if (!email.includes("@")) {
      return res.status(400).json({ message: "Invalid email" });
    }

    if (password.length < 8) {
      return res.status(400).json({ message: "Password must be at least 8 characters" });
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    await User.create({ name, email, password: hashedPassword });

    res.status(201).json({ message: "User created" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// LOGIN
app.post("/login", async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: "All fields required" });
    }

    const user = await User.findOne({ email });
    if (!user) return res.status(401).json({ message: "Invalid credentials" });

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) return res.status(401).json({ message: "Invalid credentials" });

    const token = jwt.sign({ id: user._id }, process.env.JWT_SECRET, { expiresIn: "1d" });
    res.json({ token });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// CREATE
app.post("/hikes", auth, async (req, res) => {
  try {
    const hike = await Hike.create({ ...req.body, userId: req.user.id });
    res.status(201).json(hike);
  } catch (err) {
    if (err.name === "ValidationError") {
      const messages = Object.values(err.errors).map(e => e.message);
      return res.status(400).json({ error: "Validation failed", details: messages });
    }
    res.status(500).json({ error: "Server error" });
  }
});

// GET
app.get("/hikes", auth, async (req, res) => {
  try {
    const hikes = await Hike.find({ userId: req.user.id });
    res.json(hikes);
  } catch (err) {
    res.status(500).json({ error: "Server error" });
  }
});

// UPDATE
app.put("/hikes/:id", auth, async (req, res) => {
  try {
    const hike = await Hike.findOneAndUpdate(
      { _id: req.params.id, userId: req.user.id },
      req.body,
      { new: true, runValidators: true }
    );
    if (!hike) return res.status(404).json({ message: "Hike not found" });
    res.json(hike);
  } catch (err) {
    if (err.name === "ValidationError") {
      const messages = Object.values(err.errors).map(e => e.message);
      return res.status(400).json({ error: "Validation failed", details: messages });
    }
    if (err.name === "CastError") {
      return res.status(400).json({ message: "Invalid hike ID" });
    }
    res.status(500).json({ error: "Server error" });
  }
});

// DELETE
app.delete("/hikes/:id", auth, async (req, res) => {
  try {
    const hike = await Hike.findOneAndDelete({ _id: req.params.id, userId: req.user.id });
    if (!hike) return res.status(404).json({ message: "Hike not found" });
    res.json({ message: "Deleted successfully" });
  } catch (err) {
    if (err.name === "CastError") {
      return res.status(400).json({ message: "Invalid hike ID" });
    }
    res.status(500).json({ error: "Server error" });
  }
});

const PORT = process.env.PORT || 3000;

app.listen(PORT, "0.0.0.0", () => {
  console.log(`Server running on http://localhost:${PORT}`);
});
