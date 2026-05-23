---

## Authentication

**POST** `/register`

Request body:
```json
{
  "name": "string",
  "email": "string",
  "password": "string (min 8 characters)"
}
```

Responses:
| Status | Message |
|---|---|
| 201 | `{ "message": "User created" }` |
| 400 | All fields required / Invalid email / Password too short |
| 409 | Email already exists |
| 500 | Server error |

---

**POST** `/login`

Request body:
```json
{
  "email": "string",
  "password": "string"
}
```

Responses:
| Status | Message |
|---|---|
| 200 | `{ "token": "<JWT>" }` |
| 400 | All fields required |
| 401 | Invalid credentials |
| 500 | Server error |

---

**GET** `/me` — requires auth

Responses:
| Status | Message |
|---|---|
| 200 | `{ "name": "string", "email": "string" }` |
| 401 | No or invalid token |
| 404 | User not found |
| 500 | Server error |

---

## Hikes

**GET** `/hikes` — requires auth

Returns an array of all hike records belonging to the authenticated user.

| Status | Message |
|---|---|
| 200 | Array of hike objects |
| 401 | No or invalid token |
| 500 | Server error |

---

**POST** `/hikes` — requires auth

Request body:
```json
{
  "durationSeconds": "number",
  "distanceKm": "number",
  "steps": "number",
  "calories": "number",
  "speed": {
    "avgKmh": "number",
    "maxKmh": "number"
  },
  "pace": {
    "avgMinPerKm": "number",
    "bestMinPerKm": "number"
  },
  "elevation": {
    "gainMeters": "number",
    "minMeters": "number",
    "maxMeters": "number"
  },
  "startedAt": "ISO 8601 timestamp",
  "endedAt": "ISO 8601 timestamp"
}
```

Responses:
| Status | Message |
|---|---|
| 201 | Created hike object |
| 400 | Validation failed |
| 401 | No or invalid token |
| 500 | Server error |

---

**PUT** `/hikes/:id` — requires auth

Request body: any partial or full hike fields (same schema as POST).

Responses:
| Status | Message |
|---|---|
| 200 | Updated hike object |
| 400 | Validation failed / Invalid hike ID |
| 401 | No or invalid token |
| 404 | Hike not found |
| 500 | Server error |

---

**DELETE** `/hikes/:id` — requires auth

Responses:
| Status | Message |
|---|---|
| 200 | `{ "message": "Deleted successfully" }` |
| 400 | Invalid hike ID |
| 401 | No or invalid token |
| 404 | Hike not found |
| 500 | Server error |
EOF
