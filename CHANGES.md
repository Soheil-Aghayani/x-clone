# X Clone — Complete Change History

This document records the work completed from the original JavaFX prototype through the current X-style desktop application.

## Clean public launch

- Removed all generated demo identities, posts, automatic activity, fake profile assets, and simulated chat replies.
- Added a one-time server migration that removes existing accounts and activity, then records a marker so later restarts preserve newly registered users.
- JPEG and PNG uploads are now resized/compressed before Base64 database storage; animated GIF bytes remain unchanged.
- Fixed text-only posts inheriting stale media, profile-hover flicker, profile-side suggestions, people-search routing, and oversized Explore result cards.

## 1. Original state

The project started as a small JavaFX prototype with:

- a dark mock timeline containing hard-coded posts;
- a profile page with placeholder counts and sample posts;
- separate scene navigation for Home and Profile;
- a socket server that only understood `PING`;
- login and registration controllers that bypassed the backend with mock users;
- no persistent posts, follows, reactions, bookmarks, chats, or notifications;
- emoji/text navigation icons and inline styling;
- a center-column scrollbar that left the right column visually disconnected.

## 2. Build and runtime repairs

- Standardized the project on Java 21, Maven, JavaFX 21.0.6, Gson, SQLite/Turso support, and bcrypt.
- Fixed Maven/JavaFX startup and scene navigation.
- Added a reusable `NavigationManager` that preserves the user-resized window height.
- Added minimum-window sizing and responsive center/right-column behavior.
- Added clean client/backend launch verification and smoke-test harnesses.
- Rebuilt Login and Register as high-contrast X-style dark authentication screens with provider buttons, dividers, outlined fields, validation, progress states, and clear recovery errors.
- Moved registration and login requests off the JavaFX application thread so the window remains responsive while bcrypt and network work complete.
- Added backend socket timeouts, reconnect/retry handling, explicit closed-connection errors, and verified scene-transition failure reporting.
- Cached decoded account avatars so opening the dense Home timeline after authentication no longer repeatedly decodes the same large image files.

Run the backend:

```powershell
java -cp "target/classes;$env:USERPROFILE/.m2/repository/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar;$env:USERPROFILE/.m2/repository/at/favre/lib/bcrypt/0.10.2/bcrypt-0.10.2.jar;$env:USERPROFILE/.m2/repository/at/favre/lib/bytes/1.5.0/bytes-1.5.0.jar" server.network.server
```

Run the client:

```powershell
mvn javafx:run
```

## 3. X visual system

- Rebuilt Home and Profile using X's white desktop layout.
- Added the fixed 270-pixel left navigation rail, 600-pixel primary column, and contextual right rail.
- Replaced emoji icons with the supplied SVG icon set.
- Added X-like borders, hover colors, pills, tabs, buttons, cards, menus, composer controls, and empty states.
- Added responsive behavior that hides or expands contextual columns when appropriate.
- Moved profile action buttons below the banner and kept the avatar overlapping the banner edge.
- Removed the permanent Log out button. Clicking the sidebar avatar, name, handle, or overflow button opens the account menu.
- Replaced the center-only scrollbar with a page-level scrollbar: the center and right content now move together while the left navigation stays fixed.

## 4. Fonts and international text

- Added Chirp Regular, Medium, and Bold for Latin text.
- Added Vazirmatn for Persian, Arabic, and other complex-script runs.
- Added automatic font selection for mixed-script posts, profiles, inputs, metadata, and dialogs.
- Kept hashtags and mentions clickable across Latin and complex text.

## 5. Accounts and profiles

- Replaced fake new-user follower/following totals with real zero-based social graph values.
- Added editable name, bio, location, website, birth date, professional status, avatar, and banner.
- Persisted profile edits locally across logins and application restarts.
- Added public profiles, verified badges, parody labels, location/link/joined metadata, and follow/message/subscribe controls.
- Added working Posts, Replies, Media, Likes, Followers, and Following views.
- Added pinned-post rendering and profile post ordering.
- Added hover cards and clickable display names, handles, mentions, and avatars.
- Added fictional profile photography for Layla Rahimi, Marcus Cole, Mina Park, Daniel Hart, Sofía Reyes, and Arjun Mehta.
- Kept the supplied Elon Musk and parody-account profile assets.
- Reused the signed-in user's saved profile photo in the Home composer, Home/Profile sidebar account card, and modal composer instead of leaving those locations on the initial-letter fallback.

## 6. Timeline and posts

- Replaced the four-post demo with a dense, varied seeded timeline covering software, design, games, science, sports, space, and multilingual conversation.
- Added realistic engagement and view counts to seeded posts.
- Added **For you** ranking and a chronological **Following** feed.
- Added live relative timestamps that update every second.
- Added post-detail pages with complete reply threads and view tracking.
- Added working replies, quote posts, repost/undo-repost, likes, bookmarks, and share-link copying.
- Added swipe/scale/fade feedback for increased repost and like counts.
- Filled the heart icon with X pink when a post is liked; unliked posts use the outline icon.
- Added screen-reader labels that expose reply, repost, like, unlike, bookmark, and remove-bookmark states with their current counts.
- Added owner menus for pinning, unpinning, and deleting posts.
- Added viewer menus for follow/unfollow, Not interested, mute/unmute, and report.
- Added clickable hashtags, hashtag timelines, clickable mentions, and quoted-post previews.
- Added post hover states and reaction-specific hover colors.

## 7. Composer

- Added the X-style inline composer and reusable borderless modal composer.
- Added the 280-character limit with the exact warning behavior:
  - the counter appears at 20 remaining;
  - warning color is `#fddd3e`;
  - zero and negative values use `#f42330`;
  - the ring remains through `-9` and disappears after it;
  - over-limit text remains editable while Post is disabled.
- Added image and GIF selection, attachment removal, location insertion, writing prompts, and mixed-script fonts.
- Added persistent Drafts behavior in the modal composer.
- Added polls with two to four choices, configurable duration, one vote per account, percentages, totals, and final states.
- Added reply and quote composers with post context and discard behavior for X, Escape, and close controls.

## 8. Search and discovery

- Added Explore sections for For you, Trending, News, Sports, and Entertainment.
- Added search across post text, names, usernames, hashtags, and account bios.
- Added Top, Latest, People, and Media result filters.
- Added clickable account results with follow buttons and real avatars.
- Added populated trends and Who to follow widgets.

## 9. Notifications

- Added All, Priority, and Mentions tabs.
- Added notifications for likes, reposts, replies, follows, and mentions.
- Added unread styling, read tracking, mark-as-read behavior, actor avatars, excerpts, timestamps, and navigation back to the related post/profile.
- Preserved the requested X empty-state copy for empty tabs.

## 10. Bookmarks, Creator Studio, Premium, Grok, and More

- Added persistent bookmarks and bookmark search.
- Added Creator Studio navigation and creator-tool cards.
- Added a Premium feature overview.
- Added a local Grok-style question workspace for interface completeness.
- Added the More menu with Lists, Communities, Monetization, Settings and privacy, and Help Center destinations.

## 11. Chat

- Added the requested passcode onboarding and four-digit setup.
- Added persistent direct conversations and messages.
- Added All, Unread, Direct, and Groups filters.
- Added conversation search, unread states, new-chat account selection, message bubbles, sending with Enter, and automatic scrolling to the latest message.

## 12. Persistence and backend authentication

- Replaced login and registration mock bypasses with real socket requests.
- Added durable backend accounts with unique username/email validation.
- Added bcrypt password hashing, wrong-password rejection, login sessions, and 30-day session metadata.
- Added atomic JSON persistence for client social state and chat state.
- Added automatic migration that enriches older four-post state files without deleting user-created content.
- Rejects missing, unreadable, or failed profile images before creating JavaFX image patterns, preventing Profile from crashing when an account references a moved local file.
- Added a self-contained Windows portable build. The packaged executable bundles Java/JavaFX, starts its local backend automatically, stores errors in `%USERPROFILE%\.x-clone\client-error.log`, and does not require Maven or a JDK on the tester's computer.

Runtime data locations:

- `%USERPROFILE%\.x-clone\social-state.json`
- `%USERPROFILE%\.x-clone\chat-state.json`
- `%USERPROFILE%\.x-clone-server\accounts.json`

## 13. Verification

The project has dedicated smoke coverage for:

- loading Home and Profile FXML;
- character-limit warning, zero, and negative states;
- responsive and flexible composers;
- post/reply/quote submit and discard flows;
- edit-profile save and cancel;
- profile navigation and hover cards;
- passcode creation and Chat unlock;
- persistent posts, replies, follows, reactions, polls, notifications, and chats;
- backend registration, bcrypt authentication rejection, login, and session creation;
- page-level scroll ownership and fixed-left-sidebar behavior;
- filled-heart state changes;
- rendered visual snapshots of Home, Profile, discovery centers, composers, and public profiles.

## 14. Deliberate local-clone boundaries

This is a local desktop clone, not a connection to X's production services. Real X payments, ad delivery, email/SMS verification, live Spaces audio infrastructure, and the production Grok service require external APIs and are represented locally rather than impersonated.

## 15. Chat visual polish and passcode security workflow

- Implemented persistent client-side passcode storage mapped by username using secure BCrypt hashing in `chat-state.json`.
- Added an inline lock panel that prompts the user to enter and verify their passcode to unlock encrypted chats, replacing repeated onboarding prompts.
- Bound the chat container height to the window viewport height, fixing the chat header and input composer in place while making the chat list and conversation bubbles scroll independently.
- Added highlight styling for the selected conversation row in the DM list.
- Rendered small circular sender avatars next to received message bubbles, and participant details in the chat pane header.
- Removed "Sign in with Google" and "Sign in with Apple" authentication buttons and the "or" divider from the Login screen.
- Added Twitter-style compact count formatting (e.g. 18.4K, 2.4M) for post engagement statistics (likes, retweets, replies) and view metrics.
- Wired the "Show more" link in the "You might like" widget to a new "Who to follow" Explore tab, listing all suggested accounts with full follow/unfollow capability and profile navigation.
- Constrained the width of suggested accounts list rows to 650px maximum to prevent awkward stretching when the right sidebar is hidden.
- Dynamized news and trend items in the Explore view and the right sidebar widget to be clickable, triggering an automatic post search for the selected term.
- Replaced the native system passcode setup dialog with a premium, inline "Create Passcode" screen that renders smoothly inside the chat timeline container.
- Fixed passcode check logic to correctly lookup persisted passcode hashes client-side on subsequent loads and show the unlock screen rather than forcing onboarding.
- Styled conversation list rows with explicit dark text to remain readable when selected against the light gray background.
- Added margin padding to message scroll boxes to prevent sent and received text bubbles from clipping the borders.
- Appended local time indicators and Sent/Seen read checkmarks under messages in the conversation thread.
- Simulated interactive chatbot auto-replies after a short delay when sending a message to a mock account.
- Removed "Grok", "Creator Studio", and "Premium" buttons from the left navigation sidebar.
- Refactored explore and search section tabs: wrapped each tab button in an equally-stretching, full-width click wrapper and centered the content-bound tab buttons inside. This ensures the blue active indicator line is centered and matches the exact width of the tab label text (e.g. "Who to follow") instead of stretching across the entire column.
- Adjusted header container padding to add extra spacing on the right side, pushing the settings gear icon away from the border and vertical scrollbar.
- Hooked up the profile metadata website link text to launch fully-qualified URL targets (e.g. "https://layla.dev") in the user's system default web browser on click, adding visual hover feedback.
- Cleaned up sidebar navigation on the profile layout screen to remove "Grok", "Creator Studio", and "Premium" options consistently.
- Programmatically drew and saved a high-resolution 256x256 custom 𝕏 logo PNG (with a transparent hollow cutout) to resources, registering it as the window title bar and taskbar icon for the JavaFX application.
- Fixed explore/search tab weights: set inactive tabs to regular weight (`normal`) and constrained the bold weight exclusively to the active/selected tab.
- Fixed headline hover font weight reset: defined dedicated CSS classes for discovery news/trends headlines and bound the bold font weight styling directly in stylesheet rules to prevent JavaFX from resetting it on hover.
- Populated contextual seed posts matching the Explore section news headlines, trends, and hashtags to ensure clicking any card yields relevant search feeds rather than empty result placeholders.
- Fixed message bubble sizing: set `fillWidth` to `false` on the chat message wrapper. This prevents short text bubbles (like "Hi") from stretching unnecessarily to match the width of their sibling timestamp labels.
- Centered passcode pages: configured the parent wrappers in `renderChatOnboarding()`, `renderChatUnlock()`, and `renderChatPasscodeSetup()` to grow horizontally (`ALWAYS`) and stretch to fill the center area, centering the panels perfectly in the viewport instead of being left-aligned.
- Redesigned dropdown menus: styled JavaFX `ContextMenu` with premium rounded corners (16px), soft shadow, inner item padding, and a 10px rounded background radius for active rows to replace harsh, square-cornered selection boxes.
- Created custom New Chat Modal: replaced the outdated, native JavaFX `ChoiceDialog` with a custom floating overlay modal dialog featuring a search bar, scrollable account list displaying avatars, names, handles, and premium CSS styles.
- Fixed chat modal label layout: set `HBox.setHgrow(textDetails, Priority.ALWAYS)` and explicitly styled the text color of the labels. This prevents JavaFX from squishing the text container to zero width or defaulting text to transparent/white.
- Redesigned scrollbars globally: customized all JavaFX `ScrollPane` vertical and horizontal scrollbars in `twitter.css` to be thin, modern, and rounded (using a slate thumb with transparent track), replacing thick native operating system scrollbars.
- Replaced smashed text chevron: swapped out the distorted unicode "⌄" subscript text character from the chat filter dropdown button with a crisp, high-quality SVG chevron icon aligned cleanly to the right of the text.
- Redesigned chat filter dropdown pill: styled `.chat-filter-button` in `twitter.css` with a thin, light-gray border (#cfd9de) and an elegant light-gray hover background.
- Fixed Profile Hover Card blinking: patched `ProfileHoverCard.java` to check `popup.isShowing()` before playing show transitions or calling `popup.show()`. Also reduced the vertical display gap from 6px to 2px to ensure seamless cursor transitions between the trigger text and card popover.
- Fixed new chat button availability: added a permanent "New Chat" icon button (`messages-plus-icon.svg`) in the Chat List header (next to the filter pill). This allows starting new conversations at any time, even when a conversation is actively selected.
- Implemented Chat Deletion: added `deleteConversation(long id)` in `ChatStore.java` to remove conversations from database records and persist state.
- Created Chat Header Options: added a modern options button (`more-horizontal-filled-icon.svg`) in the conversation thread pane header. Clicking it opens a dropdown menu containing a red-themed "Delete conversation" option.
- Added Confirmation Dialog: wired the delete menu option to launch a styled JavaFX Confirmation Dialog before removing the chat, resetting selection, and refreshing the list.
- Implemented Chat Pinning: added backend state tracking and sorting comparisons in `ChatStore.java` and `FeedController.java`. Users can pin conversations from the options menu to permanently anchor them at the top of the Chat list (indicated by a small bookmark pin icon).
- Implemented Chat Muting: users can mute/unmute notification states from the options dropdown menu (indicated by a small bell icon on the chat row).
- Added View Profile navigation: added a "View profile" shortcut in the options dropdown to instantly transition to the other participant's profile view.
- Rebuilt Post Drafts System: upgraded single-draft storage to support saving and loading multiple drafts per user. Designed a backward-compatible serialization schema using JSON arrays stored in the existing local cache.
- Added Close Confirmation Dialog: configured the post composer modal's close button and Escape key press to trigger a confirmation alert ("Save post?") if there is unsaved text or media, letting users save or discard their work.
- Developed Drafts Viewer Modal: wired the "Drafts" button in the composer header to open a custom, scrollable list view of all saved drafts with details (snippets, timestamps, media attachment labels), options to delete them individually, and click-to-load functionality.




















## Durable media and safe chat replies

- Image and GIF attachments are now copied into X Clone's private media library instead of retaining a fragile link to the original file.
- Missing legacy media now shows a compact unavailable notice instead of an empty, oversized panel.
- Automatic chat replies are restricted to explicitly seeded NPC accounts. Real registered users never receive simulated messages on their behalf.
- Legacy canned replies that were previously inserted into real-to-real conversations are removed when chat state is loaded.

## Dynamic notifications and X Chat settings

- Unread notification totals now update the sidebar bell badge and the window title, and clear dynamically when notifications are read.
- The X Chat filter menu now includes All, Unread, Direct, Groups, Settings, and Mark all as read with dedicated icons.
- Chat settings now persist message-request permissions, subscriber messaging, media retention, and debug-log preferences per account.
- Added a Change Passcode screen and a safe cached-media cleanup action that does not delete post or profile uploads.
- User-provided SVGs were deduplicated and organized under application resources; the project icon was moved out of the root folder.

## X-style dialogs and password visibility

- Replaced the remaining native Windows alerts for post deletion, report confirmation, draft handling, cached-media cleanup, and conversation deletion with one borderless X-style modal.
- Added accessible show/hide password controls to both Sign in and Sign up, preserving the password text, focus, and caret position while toggling.
- Organized the supplied eye artwork into normalized application SVG resources and omitted the redundant extra eye variant.

## Consistent profile identity links

- Made post avatars navigate to the author profile, matching the existing display-name and username behavior.
- Made hover-card avatars, display names, and usernames open the represented profile directly.
- Applied the same identity-link behavior to suggestion cards, notification actors, chat participants, conversation headers, and received-message avatars.
- Profile-link clicks now consume the mouse event so they do not accidentally open a post or conversation underneath.
