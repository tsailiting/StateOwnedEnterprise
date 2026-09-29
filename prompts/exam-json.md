# Permanent System Prompt & Standard Operating Procedure: Exam Seed JSON Conversion

This document defines the permanent rules, workflows, and standard operating procedures for processing official Taiwan state-owned enterprise exam source files (PDFs, official answer keys, errata notes) into `StateOwnedEnterprise` seed JSON files.

---

## 1. Core Directives & Sources of Truth

1. **Authoritative Sources**:
   - Official exam question papers (PDFs), official answer keys (標準答案), and official errata notes (更正答案 / 疑義處理 / 備註) are authoritative.
2. **Year Resolution**:
   - The official document header text inside the exam paper overrides folder names or file paths for determining `year` and `examName`.
3. **Strict Verbatim Text**:
   - Preserve exact question stem and option wording. Never paraphrase, simplify, or invent text.
4. **Official Answer Key Priority**:
   - Always use official answer keys published by official portals (Ministry of Economic Affairs, Taiwan Power Company, etc.).
   - Never solve questions independently to substitute for official answer keys.
5. **Official Errata Priority**:
   - Official errata, corrections, or question wording updates supersede original question paper text or preliminary answer keys.
6. **Explanations Rule**:
   - Set `"explanation": null` unless the official source explicitly includes an explanation text. Never generate AI/LLM explanations.
7. **Flag Ambiguities**:
   - If a scan is unreadable, an answer key is missing, or an exam uses a non-multiple-choice format (such as essay questions / 科目B 非選擇題), stop immediately and flag the issue for human review instead of guessing.
8. **Mandatory Two-Phase Workflow**:
   - **Phase 1 (Analysis & Report)**: Parse source PDF/keys, verify year, category, subjects, question ranges, errata, and stop conditions. Present report and stop.
   - **Phase 2 (Generation)**: Generate JSON files only after explicit user approval.
9. **Project Code Isolation**:
   - Do not modify Room schema, DB version, Kotlin source files, Gradle build files, or existing validated seed JSON files when importing exam data.

---

## 2. Seed JSON Schemas

### A. Exam Metadata File
Path: `app/src/main/assets/seed/exams/<year>_<category>.json`

```json
{
  "year": 112,
  "category": "人資",
  "examName": "經濟部所屬事業機構112年新進職員甄試",
  "subjects": [
    { "name": "企業管理", "sortOrder": 1 },
    { "name": "法學緒論", "sortOrder": 2 }
  ]
}
```

### B. Question File
Path: `app/src/main/assets/seed/questions/<year>_<category>_<subject>.json`

```json
{
  "year": 112,
  "category": "人資",
  "subject": "企業管理",
  "questions": [
    {
      "questionNumber": 1,
      "stem": "題目內文...",
      "options": {
        "A": "選項A內文",
        "B": "選項B內文",
        "C": "選項C內文",
        "D": "選項D內文"
      },
      "correctOptions": ["D"],
      "isBonus": false,
      "explanation": null
    }
  ]
}
```

---

## 3. Data Convention Rules

- **`correctOptions`**:
  - Array of strings. Every element must be one of `"A"`, `"B"`, `"C"`, `"D"`.
  - Must not contain duplicates (e.g. `["B", "B"]` is invalid).
  - Single answer: `["B"]`.
  - Multiple accepted answers: `["B", "D"]`.
- **Bonus Questions (`isBonus`)**:
  - Official "一律給分" / "送分" questions: set `"isBonus": true` and `"correctOptions": []`.
  - Normal questions: set `"isBonus": false` and `"correctOptions"` must contain at least one valid option (`["A"]`, `["B"]`, etc.).
- **Validation**:
  - Every generated seed file must pass `python3 scripts/validate_exam_json.py`.
