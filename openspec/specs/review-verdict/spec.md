# review-verdict Specification

## Purpose
Defines the `ReviewVerdict` and findings produced by a technically successful `REVIEW` stage, the exact structured-JSON extraction contract used to obtain them from the reviewer agent's output, how the verdict is distinguished from technical stage execution success, and the fail-closed handling required when extraction does not succeed.

## Requirements

### Requirement: Review verdict is distinct from stage execution success
A `REVIEW` stage outcome SHALL distinguish two independent facts: whether the review process executed successfully (no crash, timeout, or malformed agent output), and, only when it did, the reviewer's judgment expressed as a `ReviewVerdict` of either `APPROVED` or `CHANGES_REQUIRED`, together with a findings list. A technical execution failure of the `REVIEW` stage SHALL NOT be represented as, or treated equivalently to, a `CHANGES_REQUIRED` verdict.

#### Scenario: successful review with no defects found
- **WHEN** the `REVIEW` stage executes successfully and its output's extracted `verdict` is `"APPROVED"`
- **THEN** the stage outcome records successful execution and a `ReviewVerdict` of `APPROVED`

#### Scenario: successful review with defects found
- **WHEN** the `REVIEW` stage executes successfully and its output's extracted `verdict` is `"CHANGES_REQUIRED"`
- **THEN** the stage outcome records successful execution, a `ReviewVerdict` of `CHANGES_REQUIRED`, and the extracted findings list

#### Scenario: technical failure carries no verdict
- **WHEN** the `REVIEW` stage's execution fails for a technical reason (process crash, timeout, or an agent run reported as a failure)
- **THEN** the stage outcome records the execution as unsuccessful and carries no `ReviewVerdict` and no findings

### Requirement: Verdict extraction reads the reviewer's entire final text as one candidate JSON document
Verdict and findings extraction SHALL operate on the reviewer agent's entire final text as a single unit, never by scanning for a keyword substring anywhere within it. Extraction SHALL locate the candidate JSON document as follows: if the text contains exactly one fenced code block tagged ```` ```json ````, that block's content is the candidate document; if the text contains no such fenced block, the entire trimmed text is the candidate document; if the text contains more than one such fenced block, extraction SHALL fail as ambiguous.

#### Scenario: single fenced JSON block is used as the candidate document
- **WHEN** the reviewer's final text contains exactly one ```` ```json ```` fenced block
- **THEN** extraction uses that block's content, and only that content, as the candidate JSON document

#### Scenario: no fenced block falls back to the whole trimmed text
- **WHEN** the reviewer's final text contains no ```` ```json ```` fenced block
- **THEN** extraction treats the entire trimmed final text as the candidate JSON document

#### Scenario: multiple fenced blocks fail extraction as ambiguous
- **WHEN** the reviewer's final text contains more than one ```` ```json ```` fenced block
- **THEN** extraction fails, and the failure is treated as a technical failure of the `REVIEW` stage outcome, not as a verdict

#### Scenario: extraction never substring-matches a verdict keyword
- **WHEN** the reviewer's final text contains the word "APPROVED" somewhere outside any valid, uniquely-identified candidate JSON document (for example in a prose sentence) and the candidate document itself does not contain a valid `verdict` field
- **THEN** extraction fails and does not report `APPROVED` on the basis of that surrounding text

### Requirement: Candidate document must parse as a JSON object with exactly the required verdict and findings shape
The candidate JSON document SHALL parse as a single JSON object containing a `verdict` key whose value is a string exactly equal to `"APPROVED"` or `"CHANGES_REQUIRED"`, and a `findings` key whose value is a JSON array of strings (which may be empty). Any deviation from this shape SHALL cause extraction to fail.

#### Scenario: well-formed object with APPROVED verdict and empty findings succeeds
- **WHEN** the candidate document is `{"verdict": "APPROVED", "findings": []}`
- **THEN** extraction succeeds with `ReviewVerdict.APPROVED` and an empty findings list

#### Scenario: well-formed object with CHANGES_REQUIRED verdict and findings succeeds
- **WHEN** the candidate document is `{"verdict": "CHANGES_REQUIRED", "findings": ["missing null check", "untested branch"]}`
- **THEN** extraction succeeds with `ReviewVerdict.CHANGES_REQUIRED` and a findings list containing both strings

#### Scenario: document that is not a JSON object fails extraction
- **WHEN** the candidate document is a JSON array, string, number, boolean, or null instead of an object
- **THEN** extraction fails and is treated as a technical failure of the `REVIEW` stage outcome

#### Scenario: invalid JSON fails extraction
- **WHEN** the candidate document does not parse as JSON at all
- **THEN** extraction fails and is treated as a technical failure of the `REVIEW` stage outcome

#### Scenario: missing or wrong-typed verdict fails extraction
- **WHEN** the candidate document's `verdict` key is absent, is not a string, or is a string other than exactly `"APPROVED"` or `"CHANGES_REQUIRED"` (including a different case such as `"approved"`)
- **THEN** extraction fails and is treated as a technical failure of the `REVIEW` stage outcome

#### Scenario: missing or wrong-typed findings fails extraction
- **WHEN** the candidate document's `findings` key is absent, is not an array, or contains a non-string element
- **THEN** extraction fails and is treated as a technical failure of the `REVIEW` stage outcome

### Requirement: Approved verdict advances the workflow to acceptance
When a `REVIEW` stage outcome carries a `ReviewVerdict` of `APPROVED`, the orchestrator SHALL advance the workflow to the `ACCEPTANCE` stage.

#### Scenario: approved review proceeds to acceptance
- **WHEN** the `REVIEW` stage outcome carries a `ReviewVerdict` of `APPROVED`
- **THEN** the workflow moves to the `ACCEPTANCE` stage

### Requirement: Changes-required verdict returns the workflow to implementation
When a `REVIEW` stage outcome carries a `ReviewVerdict` of `CHANGES_REQUIRED`, the orchestrator SHALL return the workflow to the `IMPLEMENTATION` stage.

#### Scenario: changes-required review returns to implementation
- **WHEN** the `REVIEW` stage outcome carries a `ReviewVerdict` of `CHANGES_REQUIRED`
- **THEN** the workflow returns to the `IMPLEMENTATION` stage, carrying the extracted findings list in the recorded outcome

### Requirement: Failed extraction or technical failure fails closed
When the `REVIEW` stage's execution otherwise succeeds but extraction (per the two requirements above) fails for any reason, the orchestrator SHALL treat the outcome as a technical failure: it SHALL mark the workflow `FAILED` and SHALL NOT advance to `ACCEPTANCE` and SHALL NOT treat the outcome as `CHANGES_REQUIRED`. A technical failure of the `REVIEW` stage's execution (process crash, timeout, or failed agent run) SHALL also mark the workflow `FAILED` rather than being routed as `CHANGES_REQUIRED`.

#### Scenario: failed extraction fails closed
- **WHEN** the `REVIEW` stage executes without a technical error but extraction of its output fails for any reason defined by the extraction requirements
- **THEN** the orchestrator marks the workflow `FAILED` and does not advance to `ACCEPTANCE` or route to `IMPLEMENTATION` as a correction loop

#### Scenario: technical failure of the review stage fails closed
- **WHEN** the `REVIEW` stage's execution fails for a technical reason (process crash, timeout, or a failed agent run)
- **THEN** the orchestrator marks the workflow `FAILED` and does not route the outcome as `CHANGES_REQUIRED`
