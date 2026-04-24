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
app.use(express.json());
connectDB();

// AUTH MIDDLEWARE
const auth = (req, res, next) => {
  const authHeader = req.header("Authorization");
  if (!authHeader) return res.status(401).json({ message: "No token" });

  const token = authHeader.split(" ")[1];
  try {
    req.user = jwt.verify(token, process.env.JWT_SECRET);
    next();
  } catch (err) {
    res.status(401).json({ message: "Invalid token" });
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

    const user = await User.findOne({ email });
    if (!user) return res.status(404).json({ message: "User not found" });

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) return res.status(401).json({ message: "Invalid password" });

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
    res.status(400).json({ error: err.message });
  }
});

// GET
app.get("/hikes", auth, async (req, res) => {
  try {
    const hikes = await Hike.find({ userId: req.user.id });
    res.json(hikes);
  } catch (err) {
    res.status(500).json({ error: err.message });
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
    res.status(400).json({ error: err.message });
  }
});

// DELETE
app.delete("/hikes/:id", auth, async (req, res) => {
  try {
    const hike = await Hike.findOneAndDelete({ _id: req.params.id, userId: req.user.id });
    if (!hike) return res.status(404).json({ message: "Hike not found" });
    res.json({ message: "Deleted successfully" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});
