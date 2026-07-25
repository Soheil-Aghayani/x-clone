package server.database;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Original, reusable content for the shared demo network.
 *
 * <p>Posts are assembled from topic-specific thoughts, follow-ups, and tags.
 * The combinations provide thousands of possible posts without copying live
 * social-network content.</p>
 */
final class NpcContentBank {
    private NpcContentBank() {}

    static final List<Personality> PERSONALITIES = List.of(
            new Personality("xclone_maya", "Maya Chen",
                    "Desktop engineer. Java, calm interfaces, and bugs worth writing down.",
                    "Vancouver", Topic.DEVELOPMENT),
            new Personality("xclone_noah", "Noah Williams",
                    "Product designer working on accessible systems and thoughtful details.",
                    "London", Topic.DESIGN),
            new Personality("xclone_leila", "Leila Farahani",
                    "Astrophysicist translating big questions into clear pictures.",
                    "Tehran", Topic.SCIENCE),
            new Personality("xclone_aria", "Aria Santos",
                    "Indie game developer building small worlds with surprising rules.",
                    "São Paulo", Topic.GAMING),
            new Personality("xclone_theo", "Theo Martin",
                    "Football analyst. Shape, movement, and the decisions behind the score.",
                    "Paris", Topic.SPORTS),
            new Personality("xclone_nora", "Nora Okafor",
                    "Street photographer following light, weather, and ordinary moments.",
                    "Lagos · London", Topic.PHOTOGRAPHY),
            new Personality("xclone_sam", "Sam Rivera",
                    "Independent musician collecting sounds, arrangements, and late-night demos.",
                    "Manila", Topic.MUSIC),
            new Personality("xclone_iman", "Iman Darzi",
                    "Novelist, reader, and enthusiastic defender of marginal notes.",
                    "Tehran", Topic.BOOKS),
            new Personality("xclone_ren", "Ren Ito",
                    "Security engineer making threat models useful to the people shipping software.",
                    "Tokyo", Topic.SECURITY),
            new Personality("xclone_zara", "Zara Morgan",
                    "Founder sharing the practical lessons between an idea and a useful product.",
                    "Toronto", Topic.STARTUPS)
    );

    private static final Map<Topic, TopicCopy> COPY = Map.of(
            Topic.DEVELOPMENT, new TopicCopy(
                    List.of(
                            "The cleanest fix today was deleting an abstraction we no longer needed.",
                            "A fast interface starts with doing less work on the main thread.",
                            "Good error messages are part of the API, not decoration around it.",
                            "The tiny regression test we almost skipped caught the actual production bug.",
                            "Code review is most useful when it explains the risk behind a suggestion.",
                            "A boring migration plan is usually a sign that the system is becoming reliable.",
                            "We replaced a clever cache with a smaller query and the whole screen became faster.",
                            "The best refactor this week made the data flow visible from one file.",
                            "Accessibility bugs often reveal state-management bugs hiding underneath.",
                            "Shipping a small vertical slice taught us more than another week of architecture diagrams."
                    ),
                    List.of(
                            "What is the most useful test in your current project?",
                            "Which part would you simplify first?",
                            "What performance lesson did you learn the hard way?",
                            "Would you optimize this now or measure one more release?",
                            "What makes a code review genuinely helpful to you?",
                            "What small developer-experience improvement saved you time recently?"
                    ),
                    List.of("#JavaFX", "#Programming", "#OpenSource", "#DevLife")
            ),
            Topic.DESIGN, new TopicCopy(
                    List.of(
                            "A strong empty state explains what happened and gives one obvious next step.",
                            "Spacing is doing more work than color in this layout.",
                            "The interface felt faster as soon as the loading state stopped moving everything around.",
                            "A destructive action should look different before the confirmation dialog appears.",
                            "The best component rule leaves room for context instead of erasing judgment.",
                            "Keyboard navigation exposed three interactions that were visually obvious but structurally hidden.",
                            "We removed two borders and the hierarchy became much clearer.",
                            "A useful design system reduces repeated decisions without making every page identical.",
                            "The mobile layout improved when we stopped treating it as a compressed desktop screen.",
                            "Microcopy is interface design; one precise sentence can prevent an entire support flow."
                    ),
                    List.of(
                            "Which detail makes an interface feel trustworthy to you?",
                            "Do you begin with spacing, type, or color?",
                            "What empty state have you seen done particularly well?",
                            "Where should this interaction reveal more context?",
                            "Which accessibility check belongs in every design review?",
                            "What would you remove from this screen first?"
                    ),
                    List.of("#Design", "#UX", "#Accessibility", "#ProductDesign")
            ),
            Topic.SCIENCE, new TopicCopy(
                    List.of(
                            "The night sky is a record of light arriving from different moments in history.",
                            "A good scientific model is useful because it states where its uncertainty lives.",
                            "The most interesting chart in today’s paper was the one that showed the failed prediction.",
                            "Small improvements in measurement can change which questions researchers are able to ask.",
                            "The new telescope image is beautiful, but the calibration work behind it is the real story.",
                            "Climate data becomes easier to understand when the baseline and time scale stay visible.",
                            "A null result can still save the next team months of work when it is documented clearly.",
                            "The boundary between biology and computing keeps producing surprisingly practical tools.",
                            "A simulation is an argument about assumptions, not a replacement for observation.",
                            "The best science communication separates what we know from what we are still testing."
                    ),
                    List.of(
                            "Which recent discovery sent you down a research rabbit hole?",
                            "What scientific idea deserves a clearer public explanation?",
                            "Would you rather visit an observatory or a deep-ocean lab?",
                            "Which assumption would you test first?",
                            "What chart type makes uncertainty easiest to understand?",
                            "What should the next experiment measure?"
                    ),
                    List.of("#Science", "#Space", "#Research", "#Climate")
            ),
            Topic.GAMING, new TopicCopy(
                    List.of(
                            "The first playable build replaced ten imagined problems with three real ones.",
                            "A great tutorial lets the player discover a rule seconds before they need it.",
                            "We changed one sound cue and players suddenly understood the entire encounter.",
                            "The level became more interesting when every safe route required a visible trade-off.",
                            "Animation timing can make the same input feel either responsive or strangely heavy.",
                            "The boss fight works and the music works; today they finally worked at the same time.",
                            "A memorable game world suggests stories outside the path the player can actually visit.",
                            "Difficulty feels fair when failure teaches something specific.",
                            "The prototype looked terrible and answered exactly the question we built it for.",
                            "The smallest environmental detail often does the most to make a fictional place believable."
                    ),
                    List.of(
                            "What game taught you a mechanic without explaining it?",
                            "Which tiny detail made a game world feel alive?",
                            "Do you prefer difficult encounters or difficult decisions?",
                            "What belongs in the first playable prototype?",
                            "Which game has your favorite sound design?",
                            "What makes replaying a level worthwhile?"
                    ),
                    List.of("#Gaming", "#GameDev", "#IndieGames", "#LevelDesign")
            ),
            Topic.SPORTS, new TopicCopy(
                    List.of(
                            "The scoreline hides how much this match changed after the first substitution.",
                            "The calmest player on the field made the quickest decision under pressure.",
                            "A close season is a reminder that consistency matters more than one spectacular week.",
                            "The replay looks simple only because the movement before the pass created so much space.",
                            "Great defensive performances are full of actions that prevent highlights from happening.",
                            "The final result came down to recovery, spacing, and patience rather than possession.",
                            "Young players improve quickly when they are trusted with real decisions.",
                            "The crowd understood the momentum shift before the statistics did.",
                            "A tactical adjustment at halftime changed where every important duel happened.",
                            "The best post-match analysis explains the pattern without pretending the outcome was inevitable."
                    ),
                    List.of(
                            "Who changed the match without appearing on the scoresheet?",
                            "Which statistic best explains this performance?",
                            "What tactical adjustment did you notice first?",
                            "Which young player are you watching this season?",
                            "Was this result about execution or preparation?",
                            "What makes a rivalry genuinely memorable?"
                    ),
                    List.of("#Sports", "#Football", "#MatchDay", "#Analysis")
            ),
            Topic.PHOTOGRAPHY, new TopicCopy(
                    List.of(
                            "The photograph before the celebration was stronger than the celebration itself.",
                            "Today’s best frame came after the planned shoot, when everyone thought we were finished.",
                            "Light changed the story of this street without changing a single object in it.",
                            "A quieter background gave the subject more room than any lens change could.",
                            "The imperfect reflection made this portrait feel more honest.",
                            "Arriving early matters because the useful photograph often happens before the event begins.",
                            "Editing improved when I chose frames by feeling first and sharpness second.",
                            "One step to the left removed three distractions from the composition.",
                            "Bad weather gave the scene the color palette it had been missing.",
                            "The camera settings were ordinary; noticing the moment was the difficult part."
                    ),
                    List.of(
                            "Do you plan a frame or wait for it?",
                            "Which photographer changed how you notice light?",
                            "What do you remove first when editing a composition?",
                            "Would you keep the technically perfect frame or the emotional one?",
                            "What weather makes you want to take a camera outside?",
                            "Which everyday place deserves a photo walk?"
                    ),
                    List.of("#Photography", "#StreetPhotography", "#Portrait", "#VisualStorytelling")
            ),
            Topic.MUSIC, new TopicCopy(
                    List.of(
                            "The bass line is almost invisible until it disappears, and then the whole song changes.",
                            "A restrained arrangement gave this vocal more power than another layer ever could.",
                            "The live version stretches the quiet moment just long enough to transform the chorus.",
                            "A familiar chord progression can still feel new when the rhythm tells a different story.",
                            "The best discovery today came from listening to the album after the song I originally wanted.",
                            "Production choices date quickly, but a convincing performance keeps finding new listeners.",
                            "The drummer leaves exactly enough space for every other part to breathe.",
                            "A good playlist creates transitions, not just a collection of individually good songs.",
                            "The rough demo contains a kind of urgency the polished recording wisely kept.",
                            "Headphones reveal details; speakers reveal whether the song can fill a room."
                    ),
                    List.of(
                            "Which song sounds completely different live?",
                            "What album rewards listening from beginning to end?",
                            "Which instrument do you notice first in a mix?",
                            "What is your favorite unexpected musical collaboration?",
                            "Do you organize playlists by mood, genre, or memory?",
                            "Which record has the best opening thirty seconds?"
                    ),
                    List.of("#Music", "#NowPlaying", "#Songwriting", "#Production")
            ),
            Topic.BOOKS, new TopicCopy(
                    List.of(
                            "A precise first paragraph can teach the reader how to read the entire book.",
                            "The character became believable when the draft allowed them to make the wrong choice.",
                            "A good essay changes the shape of a question before it tries to answer it.",
                            "The most useful editing pass removed explanations the scene had already earned.",
                            "World-building feels deepest when ordinary routines imply a much larger history.",
                            "The ending works because the central decision was present quietly from the beginning.",
                            "A strong sentence creates momentum without asking the reader to notice the technique.",
                            "The book stayed with me because it resisted making its most difficult conflict simple.",
                            "Reading outside a favorite genre is one of the quickest ways to notice new structures.",
                            "The second draft found its voice when it stopped trying to sound finished."
                    ),
                    List.of(
                            "Which opening line do you still remember?",
                            "What book changed your mind about a genre?",
                            "Do you edit while drafting or protect the first pass?",
                            "Which fictional place feels most complete to you?",
                            "What makes you trust a narrator?",
                            "Which book deserves a slower reread?"
                    ),
                    List.of("#Books", "#Writing", "#AmReading", "#Storytelling")
            ),
            Topic.SECURITY, new TopicCopy(
                    List.of(
                            "The safest secret is the one the client application never receives.",
                            "A permission check belongs beside the data operation, not only in the interface.",
                            "The incident review became useful when it described system conditions instead of blaming one person.",
                            "Short session lifetimes help only when renewal and revocation are designed carefully.",
                            "Logging identifiers can help debugging without logging the credentials attached to them.",
                            "The boring backup test is the moment a backup becomes a recovery plan.",
                            "Rate limits are product behavior and should produce understandable responses.",
                            "A secure default prevents more incidents than a warning most people will never read.",
                            "Threat modeling becomes practical when it starts with one concrete user journey.",
                            "Dependency updates need both speed and enough testing to avoid trading one failure for another."
                    ),
                    List.of(
                            "Which security default should more products adopt?",
                            "When did you last test restoring a backup?",
                            "What belongs in every lightweight threat model?",
                            "Which credential should this client never possess?",
                            "How do you make a permission failure understandable?",
                            "What security lesson improved your engineering process?"
                    ),
                    List.of("#Security", "#Privacy", "#AppSec", "#Engineering")
            ),
            Topic.STARTUPS, new TopicCopy(
                    List.of(
                            "The customer interview changed direction when we stopped pitching and asked for the last real example.",
                            "A smaller release exposed the important assumption before we invested in the expensive part.",
                            "Retention became easier to discuss when the team looked at user behavior instead of averages.",
                            "The roadmap improved when every item stated which uncertainty it was meant to reduce.",
                            "A manual process is sometimes the fastest honest prototype of an automated product.",
                            "The useful metric was not sign-ups; it was how many people returned without a reminder.",
                            "A clear no to one audience can make the product much better for the audience it serves.",
                            "The support inbox contains product research written in the customer’s own language.",
                            "Pricing conversations become clearer when they begin with the outcome customers already value.",
                            "The team moved faster after defining what this release explicitly would not solve."
                    ),
                    List.of(
                            "Which assumption would you test before writing more code?",
                            "What did your last customer conversation change?",
                            "Which metric best represents genuine value here?",
                            "What should stay manual for one more release?",
                            "Who is this product intentionally not for?",
                            "What is the smallest version that could teach the team something?"
                    ),
                    List.of("#Startups", "#BuildInPublic", "#Product", "#Founders")
            )
    );

    private static final List<String> REPLIES = List.of(
            "That trade-off is easy to miss until you see it in a real example.",
            "I like this framing. The constraint is doing useful work here.",
            "The second-order effect might be even more interesting than the immediate result.",
            "This is a good reminder that the simplest explanation is not always the complete one.",
            "I had the same reaction, especially to the last point.",
            "The context matters a lot here. I would love to see how this changes at a different scale.",
            "That question is going to stay with me for a while.",
            "There is probably a useful experiment hiding inside this observation.",
            "Strong point. The part people experience is often different from the part teams measure.",
            "I would add that consistency matters as much as the individual decision.",
            "This makes me want to revisit an assumption I have been carrying for too long.",
            "The quiet detail is what makes the larger pattern visible.",
            "Agreed. A concrete example makes this much easier to evaluate.",
            "I am curious where you would draw the boundary in the opposite case.",
            "That last sentence explains the whole idea better than a long checklist would."
    );

    static String post(Personality personality, Random random) {
        TopicCopy copy = COPY.get(personality.topic());
        String thought = choose(copy.thoughts(), random);
        String followUp = random.nextInt(100) < 64 ? " " + choose(copy.followUps(), random) : "";
        String tag = " " + choose(copy.tags(), random);
        return thought + followUp + tag;
    }

    static String reply(Personality personality, String targetUsername, Random random) {
        TopicCopy copy = COPY.get(personality.topic());
        String body = random.nextBoolean() ? choose(REPLIES, random) : choose(copy.followUps(), random);
        return "@" + targetUsername + " " + body;
    }

    static String media(Personality personality, Random random) {
        if (random.nextInt(100) >= 58) return null;
        List<String> media = personality.mediaUris();
        return media.get(random.nextInt(media.size()));
    }

    private static String choose(List<String> values, Random random) {
        return values.get(random.nextInt(values.size()));
    }

    record Personality(
            String username,
            String displayName,
            String bio,
            String location,
            Topic topic) {
        String slug() {
            return username.substring("xclone_".length());
        }

        String avatarUri() {
            return "/images/npc/avatars/" + slug() + ".jpg";
        }

        String bannerUri() {
            return "/images/npc/banners/" + slug() + ".jpg";
        }

        List<String> mediaUris() {
            String root = "/images/npc/media/" + slug() + "-";
            return List.of(root + "1.jpg", root + "2.jpg", root + "3.jpg");
        }
    }
    private record TopicCopy(List<String> thoughts, List<String> followUps, List<String> tags) {}
    private enum Topic { DEVELOPMENT, DESIGN, SCIENCE, GAMING, SPORTS, PHOTOGRAPHY, MUSIC, BOOKS, SECURITY, STARTUPS }
}
