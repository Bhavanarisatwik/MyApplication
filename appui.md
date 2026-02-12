UI/UX DESIGN SPECIFICATION
Personal AI Memory Engine Mobile Application
1. Product Design Philosophy

The user interface must reflect the nature of the system, which is a passive cognitive memory engine rather than a conventional recorder or assistant dashboard. The experience should feel calm, intelligent, and privacy-aware. The user should never feel like they are being actively monitored; instead, the interface should convey that the system quietly listens, understands, compresses, and remembers only meaningful insights. The visual and interaction design must communicate trust, subtlety, and intelligence, ensuring the application feels like a personal memory companion rather than a surveillance tool.

The UX should be centered around the metaphor of memory formation. Raw speech is not shown as recordings. Instead, users see structured knowledge being formed over time. The transitions between listening, processing, summarizing, and recalling must be represented through fluid animations and symbolic visual states that reflect cognition. The design must avoid clutter, avoid constant flashing indicators, and avoid intrusive notifications. It should operate quietly but visibly enough to reassure the user that the system is functioning.

2. Core Navigation Structure

The application must have a bottom navigation structure with four primary sections that map directly to the cognitive lifecycle of the system. These sections are Listening, Memory Timeline, Ask and Retrieve, and Settings and Privacy. Each tab must represent a distinct functional layer while maintaining visual consistency across the application. Navigation transitions should be smooth and continuous, creating a sense of moving across layers of memory rather than jumping between unrelated pages.

The Listening tab represents the present moment where passive capture occurs. The Memory Timeline represents compressed historical knowledge. The Ask and Retrieve tab represents semantic recall and contextual querying. The Settings and Privacy tab represents user ownership, control, and transparency of stored data.

3. Listening Screen Design

The Listening screen is the default landing page and must convey passive intelligence rather than active recording. The central element should be a softly animated waveform orb that reflects voice activity detection status. When no speech is detected, the orb should display a calm breathing animation to indicate that the system is alive but idle. When speech is detected, the waveform should expand and pulse gently to signal that meaningful speech is being captured. This animation should be subtle and not distracting, reinforcing the idea of quiet background cognition.

Below the waveform orb, a dynamic status text should indicate the current stage of the pipeline, such as listening privately, processing recent speech, or summarizing last hour’s conversations. The text should update automatically based on backend state changes but should never overwhelm the user with technical jargon. The tone must be reassuring and human-friendly.

At the bottom of the screen, a compact card should display the most recent hourly summary in a concise format. This provides feedback that the system is working without exposing raw recordings. When a new summary is generated, the card should appear with a gentle upward float animation and a subtle glow, symbolizing the formation of structured knowledge from speech.

4. Memory Timeline Screen Design

The Memory Timeline screen is the core visualization of long-term knowledge. It must present a chronological timeline organized by day, with each day acting as a collapsible node. When expanded, each day reveals a list of hourly memory cards representing compressed semantic summaries of that hour’s conversations.

Each hourly card should display key extracted attributes such as major topics discussed, important decisions, tasks identified, and overall emotional tone. These attributes should be visually differentiated using subtle icons and color accents without making the interface feel noisy. The cards should be minimal and readable, with timestamps clearly displayed to preserve temporal context.

Older memories should gradually appear slightly faded or desaturated to visually represent the retention lifecycle and natural degradation over time. Pinned memories should remain fully vibrant and highlighted with a soft accent border or glow to signal permanence. Expanding or collapsing daily nodes should be accompanied by smooth accordion animations that maintain continuity and reinforce the idea of navigating through layers of memory over time.

5. Ask and Retrieve Screen Design

The Ask and Retrieve screen must function as a contextual memory query interface rather than a generic chat assistant. At the top of the screen, there should be a clean input field allowing users to type or speak questions related to past conversations, such as inquiries about decisions, tasks, or topics discussed previously. The input interaction should feel lightweight and responsive, encouraging natural language queries without forcing rigid commands.

When a query is submitted, the system should briefly display a semantic processing animation that visually represents searching across the memory timeline. This animation should be subtle, such as lines connecting across invisible timeline nodes, implying intelligent contextual matching rather than brute-force scanning.

The response should appear in a structured card that presents a concise answer generated from relevant stored summaries. Beneath the answer, a list of source references should be displayed, showing the specific days or hourly segments from which the information was derived. Each source reference should be clickable, allowing the user to jump directly to the corresponding location in the Memory Timeline screen. This ensures transparency, traceability, and user trust in the recall mechanism.

6. Settings and Privacy Screen Design

The Settings and Privacy screen must communicate control, ownership, and transparency. It should clearly display microphone permission status, secure connection state to the orchestration service, and the linkage status to the user’s Google Drive account. Retention policies should be shown in a human-readable form, explaining how raw audio is deleted quickly, hourly summaries expire after a short duration, and daily summaries persist longer unless pinned.

The screen should also provide options to manage pinned memories, clear local cache, and toggle offline encrypted storage. Visual elements such as lock icons, shield indicators, and confirmation animations should reinforce the sense of privacy and data ownership. The user should feel fully in control of what is stored and how long it remains accessible.

7. Animation and Interaction Principles

All animations must have semantic meaning tied to the cognitive lifecycle. Listening animations correspond to voice detection events. Summary card appearance animations correspond to successful hourly summarization. Timeline fading animations correspond to retention-based deletion and degradation of older memories. Retrieval query animations correspond to semantic search across stored summaries.

Animations should always be smooth, lightweight, and purpose-driven. They must not be overly flashy or distracting. Continuous looping animations should be avoided except for subtle idle breathing effects in the listening state. Micro-interactions such as gentle scale transitions, ripple confirmations, and soft fades should be used to enhance perceived responsiveness and intelligence without compromising performance or battery efficiency.

8. UX Alignment with Backend Orchestration

The UI must mirror the backend processing pipeline exactly to maintain conceptual clarity. When the Android foreground service is capturing speech, the Listening screen should reflect active listening status. When OpenClaw is buffering transcripts and calling Sarvam AI, the status text should reflect processing activity. When hourly summarization completes, a new memory card should appear in the timeline. When retention cleanup removes outdated artifacts, the corresponding cards should fade smoothly rather than disappearing abruptly. When the user asks a question, the retrieval interface should show a brief semantic linking animation before displaying the answer grounded in stored memory summaries.

This alignment ensures that the visual experience remains consistent with the actual system behavior, reinforcing trust and reducing confusion about what the system is doing at any moment.

9. Overall User Experience Vision

The final user experience should feel like interacting with a calm, intelligent personal memory companion that quietly observes, understands, compresses, and recalls meaningful moments without overwhelming the user. The interface must feel trustworthy, elegant, and futuristic while maintaining clarity and control. Users should always feel confident that their memories are being organized intelligently, stored privately in their own cloud space, and retrieved accurately when needed, all without exposing raw recordings or creating a sense of intrusive monitoring.