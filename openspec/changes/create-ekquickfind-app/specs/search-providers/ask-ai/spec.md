# Spec Delta

## Purpose

Hands the typed text to an AI chat app in one tap.

## ADDED Requirements

### Requirement: Ask AI shortcuts
For any non-empty query the system SHALL offer "Ask ChatGPT", "Ask Claude" and "Ask Gemini" (each can be turned off in settings) in an "Ask AI" section shown as chips, never as the top result. Each opens the installed app through its web link, or the website, with the query filled in where the service supports it: ChatGPT and Claude through `?q=`; Gemini has no way to prefill, so the query is copied to the clipboard. Claude's prefill isn't documented, so its query is copied too.

#### Scenario: Ask ChatGPT
- **WHEN** the user types "best pizza in rome" and taps Ask ChatGPT
- **THEN** `https://chatgpt.com/?q=best%20pizza%20in%20rome` opens in the ChatGPT app or the browser

#### Scenario: Ask Gemini
- **WHEN** the user taps Ask Gemini
- **THEN** the question is copied and Gemini opens, ready for pasting

#### Scenario: Installed apps
- **WHEN** an assistant's app is installed
- **THEN** its chip shows the app's icon
