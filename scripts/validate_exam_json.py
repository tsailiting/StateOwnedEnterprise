#!/usr/bin/env python3
"""
Deterministic Seed JSON Validator for StateOwnedEnterprise.

Validates all exam metadata and question seed JSON files against the project schema and constraints:
- Valid JSON syntax
- Metadata fields and uniqueness
- Cross-file relation mapping (question file -> exam metadata)
- Question number uniqueness and continuity (contiguous min..max range)
- Option presence (A/B/C/D)
- correctOptions validity, non-duplication, and isBonus conventions
- Null/non-empty explanations
"""

import argparse
import json
import sys
from pathlib import Path


class SeedValidator:

    def __init__(self, seed_dir: Path, verbose: bool = False):
        self.seed_dir = seed_dir
        self.verbose = verbose
        self.exams_dir = seed_dir / "exams"
        self.questions_dir = seed_dir / "questions"

        self.errors = []
        self.warnings = []

        # State tracking
        self.exam_metadata_map = {}  # (year, category) -> list of subject names
        self.exam_metadata_files_count = 0
        self.question_files_count = 0
        self.total_questions_validated = 0
        self.exam_subject_summary = {}  # (year, category) -> dict(subject -> dict(count, range))

    def log_error(self, file_path: Path, msg: str):
        self.errors.append(f"[ERROR] {file_path.name}: {msg}")

    def log_warning(self, file_path: Path, msg: str):
        self.warnings.append(f"[WARNING] {file_path.name}: {msg}")

    def validate_all(self) -> bool:
        if not self.seed_dir.exists():
            self.errors.append(f"[ERROR] Seed directory does not exist: {self.seed_dir}")
            return False

        self._validate_exam_metadata_files()
        self._validate_question_files()
        return len(self.errors) == 0

    def _validate_exam_metadata_files(self):
        if not self.exams_dir.exists():
            self.errors.append(f"[ERROR] Exams directory missing: {self.exams_dir}")
            return

        json_files = sorted(self.exams_dir.glob("*.json"))
        self.exam_metadata_files_count = len(json_files)

        for file_path in json_files:
            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    data = json.load(f)
            except Exception as e:
                self.log_error(file_path, f"Invalid JSON syntax: {e}")
                continue

            # Check required fields
            year = data.get("year")
            category = data.get("category")
            exam_name = data.get("examName")
            subjects = data.get("subjects")

            if not isinstance(year, int) or year <= 0:
                self.log_error(file_path, f"'year' must be a positive integer, got: {year}")
            if not isinstance(category, str) or not category.strip():
                self.log_error(file_path, f"'category' must be a non-empty string, got: {category}")
            if not isinstance(exam_name, str) or not exam_name.strip():
                self.log_error(file_path, f"'examName' must be a non-empty string, got: {exam_name}")
            if not isinstance(subjects, list) or not subjects:
                self.log_error(file_path, f"'subjects' must be a non-empty list")
                continue

            key = (year, category)
            if key in self.exam_metadata_map:
                self.log_error(file_path, f"Duplicate exam metadata for year={year}, category='{category}'")
            else:
                self.exam_metadata_map[key] = []
                self.exam_subject_summary[key] = {}

            seen_subject_names = set()
            seen_sort_orders = set()

            for idx, subj in enumerate(subjects):
                if not isinstance(subj, dict):
                    self.log_error(file_path, f"Subject item at index {idx} must be an object")
                    continue
                name = subj.get("name")
                sort_order = subj.get("sortOrder")

                if not isinstance(name, str) or not name.strip():
                    self.log_error(file_path, f"Subject name at index {idx} must be a non-empty string")
                elif name in seen_subject_names:
                    self.log_error(file_path, f"Duplicate subject name '{name}' in exam metadata")
                else:
                    seen_subject_names.add(name)
                    if key in self.exam_metadata_map:
                        self.exam_metadata_map[key].append(name)

                if not isinstance(sort_order, int) or sort_order <= 0:
                    self.log_error(file_path, f"Subject sortOrder at index {idx} must be positive int")
                elif sort_order in seen_sort_orders:
                    self.log_error(file_path, f"Duplicate subject sortOrder {sort_order} in exam metadata")
                else:
                    seen_sort_orders.add(sort_order)

    def _validate_question_files(self):
        if not self.questions_dir.exists():
            self.errors.append(f"[ERROR] Questions directory missing: {self.questions_dir}")
            return

        json_files = sorted(self.questions_dir.glob("*.json"))
        self.question_files_count = len(json_files)

        for file_path in json_files:
            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    data = json.load(f)
            except Exception as e:
                self.log_error(file_path, f"Invalid JSON syntax: {e}")
                continue

            year = data.get("year")
            category = data.get("category")
            subject = data.get("subject")
            questions = data.get("questions")

            if not isinstance(year, int) or year <= 0:
                self.log_error(file_path, f"'year' must be a positive integer, got: {year}")
            if not isinstance(category, str) or not category.strip():
                self.log_error(file_path, f"'category' must be a non-empty string, got: {category}")
            if not isinstance(subject, str) or not subject.strip():
                self.log_error(file_path, f"'subject' must be a non-empty string, got: {subject}")

            key = (year, category)
            if key not in self.exam_metadata_map:
                self.log_error(
                    file_path,
                    f"No matching Exam metadata declared for year={year}, category='{category}'"
                )
            elif subject not in self.exam_metadata_map[key]:
                self.log_error(
                    file_path,
                    f"Subject '{subject}' is not declared in Exam metadata for year={year}, category='{category}'"
                )

            if not isinstance(questions, list) or not questions:
                self.log_error(file_path, f"'questions' must be a non-empty list")
                continue

            q_numbers = []
            valid_options_set = {"A", "B", "C", "D"}

            for idx, q in enumerate(questions):
                if not isinstance(q, dict):
                    self.log_error(file_path, f"Question at index {idx} must be an object")
                    continue

                q_num = q.get("questionNumber")
                stem = q.get("stem")
                options = q.get("options")
                correct_options = q.get("correctOptions")
                is_bonus = q.get("isBonus")
                explanation = q.get("explanation")

                # questionNumber check
                if not isinstance(q_num, int) or q_num <= 0:
                    self.log_error(file_path, f"Question at index {idx} has invalid questionNumber: {q_num}")
                else:
                    q_numbers.append(q_num)

                # stem check
                if not isinstance(stem, str) or not stem.strip():
                    self.log_error(file_path, f"Q{q_num} stem must be a non-empty string")

                # options check
                if not isinstance(options, dict):
                    self.log_error(file_path, f"Q{q_num} 'options' must be an object")
                else:
                    if set(options.keys()) != valid_options_set:
                        self.log_error(file_path, f"Q{q_num} 'options' keys must be exactly ['A','B','C','D']")
                    for opt_key in ["A", "B", "C", "D"]:
                        opt_val = options.get(opt_key)
                        if not isinstance(opt_val, str) or not opt_val.strip():
                            self.log_error(file_path, f"Q{q_num} option '{opt_key}' must be a non-empty string")

                # isBonus check
                if not isinstance(is_bonus, bool):
                    self.log_error(file_path, f"Q{q_num} 'isBonus' must be a boolean (true/false)")
                    is_bonus = False

                # correctOptions check
                if not isinstance(correct_options, list):
                    self.log_error(file_path, f"Q{q_num} 'correctOptions' must be a list")
                else:
                    # check element types and valid set
                    has_invalid_elem = False
                    for opt in correct_options:
                        if not isinstance(opt, str) or opt not in valid_options_set:
                            has_invalid_elem = True
                            self.log_error(file_path, f"Q{q_num} 'correctOptions' contains invalid value: {opt}")

                    # check duplicates
                    if len(correct_options) != len(set(correct_options)):
                        self.log_error(file_path, f"Q{q_num} 'correctOptions' contains duplicate values: {correct_options}")

                    # bonus vs normal validation rules
                    if is_bonus:
                        if len(correct_options) != 0:
                            self.log_error(
                                file_path,
                                f"Q{q_num} has isBonus=true, so 'correctOptions' must be [], got: {correct_options}"
                            )
                    else:
                        if len(correct_options) == 0:
                            self.log_error(
                                file_path,
                                f"Q{q_num} has isBonus=false, but 'correctOptions' is empty []"
                            )

                # explanation check
                if explanation is not None:
                    if not isinstance(explanation, str) or not explanation.strip():
                        self.log_error(file_path, f"Q{q_num} 'explanation' must be null or non-empty string")

            # Question number continuity and duplicate checks
            if q_numbers:
                sorted_nums = sorted(q_numbers)
                min_num = sorted_nums[0]
                max_num = sorted_nums[-1]

                # Check for duplicates
                duplicates = [num for num in set(q_numbers) if q_numbers.count(num) > 1]
                if duplicates:
                    self.log_error(file_path, f"Duplicate question numbers found: {duplicates}")

                # Check continuity from min to max
                expected_range = list(range(min_num, max_num + 1))
                if sorted_nums != expected_range:
                    missing = set(expected_range) - set(q_numbers)
                    self.log_error(
                        file_path,
                        f"Question numbers are not contiguous in range Q{min_num}..Q{max_num}. Missing: {sorted(missing)}"
                    )

                # Record statistics
                self.total_questions_validated += len(q_numbers)
                if key in self.exam_subject_summary:
                    self.exam_subject_summary[key][subject] = {
                        "count": len(q_numbers),
                        "range": f"Q{min_num}..Q{max_num}"
                    }

    def print_report(self):
        print("=" * 60)
        print(" SEED JSON VALIDATION REPORT")
        print("=" * 60)
        print(f"Seed Directory: {self.seed_dir.resolve()}")
        print(f"Exam Metadata Files: {self.exam_metadata_files_count}")
        print(f"Question Files:      {self.question_files_count}")
        print(f"Total Questions:     {self.total_questions_validated}")
        print("-" * 60)

        print("PER-EXAM SUMMARY:")
        for (year, category), subjects in self.exam_subject_summary.items():
            print(f"  • {year} 年 {category}:")
            exam_total = 0
            for subj_name, info in subjects.items():
                print(f"    - {subj_name}: {info['count']} 題 ({info['range']})")
                exam_total += info["count"]
            print(f"    Total: {exam_total} 題")

        print("-" * 60)
        print(f"Warnings: {len(self.warnings)}")
        for w in self.warnings:
            print(f"  {w}")

        print(f"Errors:   {len(self.errors)}")
        for e in self.errors:
            print(f"  {e}")

        print("=" * 60)
        if len(self.errors) == 0:
            print(" RESULT: PASSED (All seed JSON files are valid)")
        else:
            print(" RESULT: FAILED (Validation errors detected)")
        print("=" * 60)


def main():
    parser = argparse.ArgumentParser(description="Validate StateOwnedEnterprise seed JSON files.")
    default_seed_dir = Path(__file__).parent.parent / "app" / "src" / "main" / "assets" / "seed"
    parser.add_argument(
        "--dir",
        type=Path,
        default=default_seed_dir,
        help="Path to seed directory containing exams/ and questions/"
    )
    parser.add_argument(
        "-v", "--verbose",
        action="store_true",
        help="Enable verbose output"
    )

    args = parser.parse_args()

    validator = SeedValidator(seed_dir=args.dir, verbose=args.verbose)
    success = validator.validate_all()
    validator.print_report()

    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
