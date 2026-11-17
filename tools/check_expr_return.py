#!/usr/bin/env python3
"""Fail if a `return` sits inside an expression-body Kotlin function.

Local kotlinc typecheck passes this, but Gradle's compileDebugKotlin rejects it
(lesson from Blocker v2, 2026-10-25: `return` inside UiKit.versionLine).

Heuristic: track brace depth. A `fun ... = <expr>` line with no `{` before the
end of the declaration opens an expression body; any `return` before the next
top-level declaration at the same depth is a hit. Lambdas/blocks inside the
expression (e.g. `= run { return ... }`) are real Kotlin errors too when the
return targets the function, so flagging them is correct.
"""
import re
import sys

FUN_RE = re.compile(r'^\s*(?:public|private|internal|protected|override|suspend|inline|tailrec|operator|infix|open|abstract)?\s*fun\b')
RETURN_RE = re.compile(r'\breturn\b')


def check_file(path):
    hits = []
    with open(path, encoding='utf-8') as f:
        lines = f.readlines()
    i = 0
    while i < len(lines):
        line = lines[i]
        if FUN_RE.match(line):
            # Is this an expression-body fun? Look for '=' before any '{' on the
            # declaration (may span lines until '{' or '=' at depth 0).
            # '=' داخل پرانتز (مقدار پیش‌فرض پارامتر) نشانه بدنه عبارتی نیست؛
            # فقط '=' در عمق پرانتز صفر شمرده می‌شود.
            decl = line
            j = i
            brace_seen = False
            eq_seen = False
            paren_depth = 0
            while j < len(lines):
                decl += lines[j] if j != i else ''
                for ch in lines[j]:
                    if ch == '(':
                        paren_depth += 1
                    elif ch == ')':
                        paren_depth -= 1
                    elif ch == '{':
                        brace_seen = True
                        break
                    elif ch == '=' and paren_depth == 0 and not brace_seen:
                        eq_seen = True
                if brace_seen or eq_seen or lines[j].rstrip().endswith(('=', '{')):
                    break
                if j > i and lines[j].strip() == '':
                    break
                j += 1
                if j - i > 5:
                    break
            if eq_seen and not brace_seen:
                # Expression body: scan until the expression ends. A `return`
                # anywhere in it is a compile error for the enclosing fun.
                k = i
                depth = 0
                while k < len(lines):
                    text = lines[k]
                    if k != i and RETURN_RE.search(text):
                        hits.append((k + 1, text.strip()))
                    depth += text.count('{') - text.count('}')
                    # Expression ends at a blank line or a new top-level decl
                    # after the first line, when braces are balanced.
                    if k > i and depth <= 0 and (
                        text.strip() == '' or FUN_RE.match(text) or text.startswith(('class ', 'object ', '}', 'private ', 'public ', 'internal '))
                    ):
                        break
                    k += 1
                    if k - i > 40:
                        break
        i += 1
    return hits


def main():
    import glob
    failed = False
    for root in sys.argv[1:]:
        for path in sorted(glob.glob(root + '/**/*.kt', recursive=True)):
            for lineno, text in check_file(path):
                print(f'{path}:{lineno}: return inside expression-body function: {text}')
                failed = True
    sys.exit(1 if failed else 0)


if __name__ == '__main__':
    main()
