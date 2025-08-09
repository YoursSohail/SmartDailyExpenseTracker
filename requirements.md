# Smart Daily Expense Tracker — Full Module (AI-First)

## 📌 Title
Build a Full-featured “Smart Daily Expense Tracker” Module for Small Business Owners.

## 🎯 Context
This module will help small business owners digitize their daily expense tracking process.  
Many expenses are unrecorded or scattered across WhatsApp messages or paper notes, leading to poor cash flow visibility.  
The goal is to enable users to **capture, view, analyze, and export** expense records easily and intelligently.

AI tools must be used throughout the development process to accelerate, generate, or optimize parts of the workflow.

---

## ⚙️ Module Overview
- **Platform**: Android
- **UI Framework**: Jetpack Compose (preferred) or XML-based views
- **Architecture**: Clean MVVM

---

## 🛠️ Required Screens & Flows

### 1. Expense Entry Screen
**Fields:**
- **Title** (text)
- **Amount (₹)**
- **Category**: List — `Staff`, `Travel`, `Food`, `Utility`
- **Optional Notes** (max 100 characters)
- **Optional Receipt Image** (upload or mock)

**Features:**
- Submit button:
    - Validates input (amount > 0, title non-empty)
    - Detects duplicates before saving
    - Adds the expense
    - Shows Toast confirmation
    - Animates entry
- Show **real-time “Total Spent Today”** at the top
- Persist data locally (Room or DataStore)
- Sync logic prepared for **offline-first** (mocked)

---

### 2. Expense List Screen
**Filters & Views:**
- Default view: **Today**
- View previous dates via calendar or filter
- Group by **category** or **time** (toggle)

**Display:**
- Total count of expenses
- Total amount
- Empty state view when no expenses are found
- Supports **Light/Dark mode**
- Animations when loading or updating list

---

### 3. Expense Report Screen
**Report for last 7 days:**
- Daily totals
- Category-wise totals
- Bar chart or line chart (mocked)

**Export & Sharing:**
- PDF export simulation
- CSV export simulation
- Trigger share intent for generated reports

---

## 🔄 State Management & Data Layer
- **ViewModel + StateFlow** (or LiveData)
- Repository pattern:
    - Local data source: Room or DataStore
    - In-memory caching for quick access
- Navigation component for screen transitions

---

## ✨ Mandatory Bonus Features
- Theme switcher (Light/Dark mode)
- Persist data locally (Room/Datastore)
- Animation on adding expenses
- Duplicate expense detection
- Validation for all fields:
    - Amount > 0
    - Title non-empty
- Offline-first sync (mocked)
- Reusable UI components for repeated patterns (e.g., expense item card, dropdown, buttons)

---

## 🤖 AI Usage (Mandatory)
Use AI tools (e.g., ChatGPT, Cursor, Copilot, Gemini) for:
- UI layout ideas
- MVVM structuring
- ViewModel & Data class generation
- UX feedback & enhancements
- Prompt tuning & retries
- Code comments & README help

Keep a **log of prompts** and AI-generated suggestions.

---

## 📂 Deliverables
- ✅ Source Code (GitHub repository or ZIP file)
- ✅ AI Usage Summary
- ✅ APK or Download Link
- ✅ Screenshots
- ✅ Prompt logs

---

## 📝 Submission Requirements
- **AI Usage Summary**: 3–5 sentences describing how AI was used
- **Prompt Logs**: Include key prompts and retries
- **All optional features completed** and integrated as part of the main module
