import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A self-contained Java ChatBot with GUI and NLP-based FAQ matching.
 * Uses TF-IDF + cosine similarity on stemmed tokens.
 */
public class ChatBotApp {

    // -------- Core ChatBot Logic --------
    static class ChatBot {
        private final List<FAQ> faqs = new ArrayList<>();
        private final Set<String> stopWords = new HashSet<>(Arrays.asList(
                "a", "an", "the", "of", "for", "on", "at", "to", "in", "with",
                "without", "but", "or", "yet", "so", "as", "by", "about", "into",
                "through", "during", "including", "etc", "etc."
        ));
        private final Stemmer stemmer = new Stemmer();

        // Add a FAQ entry
        public void addFAQ(String question, String answer) {
            faqs.add(new FAQ(question, answer));
        }

        // Train the bot with a batch of FAQs (for initialisation)
        public void train(List<String[]> qaPairs) {
            for (String[] pair : qaPairs) {
                addFAQ(pair[0], pair[1]);
            }
        }

        // Get response for user input
        public String getResponse(String input) {
            if (input == null || input.trim().isEmpty()) {
                return "Please say something!";
            }

            String cleanedInput = preprocess(input);
            if (cleanedInput.isEmpty()) {
                return "I didn't understand that. Could you rephrase?";
            }

            // Compute TF-IDF vectors for the input and all FAQ questions
            List<String> allQuestions = faqs.stream()
                    .map(f -> preprocess(f.question))
                    .collect(Collectors.toList());
            allQuestions.add(cleanedInput); // add user input at the end

            // Build term-document matrix
            List<Set<String>> documentTokens = allQuestions.stream()
                    .map(text -> new HashSet<>(tokenize(text)))
                    .collect(Collectors.toList());

            // Global vocabulary
            Set<String> vocabulary = new HashSet<>();
            for (Set<String> tokens : documentTokens) {
                vocabulary.addAll(tokens);
            }

            // Compute TF-IDF vectors
            List<Map<String, Double>> tfidfVectors = new ArrayList<>();
            for (int i = 0; i < documentTokens.size(); i++) {
                Map<String, Double> vector = new HashMap<>();
                Set<String> tokens = documentTokens.get(i);
                for (String term : vocabulary) {
                    double tf = tokens.contains(term) ? 1.0 : 0.0; // binary TF for simplicity
                    double idf = Math.log((double) documentTokens.size() / 
                            (1 + documentTokens.stream().filter(doc -> doc.contains(term)).count()));
                    vector.put(term, tf * idf);
                }
                tfidfVectors.add(vector);
            }

            // User vector is the last one
            Map<String, Double> userVector = tfidfVectors.remove(tfidfVectors.size() - 1);

            // Compute cosine similarity with each FAQ vector
            double bestScore = -1;
            int bestIndex = -1;
            for (int i = 0; i < tfidfVectors.size(); i++) {
                double score = cosineSimilarity(userVector, tfidfVectors.get(i));
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = i;
                }
            }

            // Threshold (0.2 works well for short texts)
            if (bestScore > 0.2) {
                return faqs.get(bestIndex).answer;
            } else {
                return "I'm not sure about that. Please contact support or rephrase your question.";
            }
        }

        // Cosine similarity between two vectors
        private double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
            double dot = 0.0, norm1 = 0.0, norm2 = 0.0;
            for (String key : v1.keySet()) {
                double val1 = v1.getOrDefault(key, 0.0);
                double val2 = v2.getOrDefault(key, 0.0);
                dot += val1 * val2;
                norm1 += val1 * val1;
            }
            for (double val : v2.values()) {
                norm2 += val * val;
            }
            if (norm1 == 0 || norm2 == 0) return 0;
            return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
        }

        // Preprocess: tokenize, remove stopwords, stem, join
        private String preprocess(String text) {
            List<String> tokens = tokenize(text);
            tokens = tokens.stream()
                    .filter(token -> !stopWords.contains(token.toLowerCase()))
                    .map(stemmer::stem)
                    .collect(Collectors.toList());
            return String.join(" ", tokens);
        }

        // Simple tokenizer: split on non-word characters and lowercase
        private List<String> tokenize(String text) {
            if (text == null) return new ArrayList<>();
            return Arrays.stream(text.toLowerCase().split("[^a-zA-Z0-9]+"))
                    .filter(t -> !t.isEmpty())
                    .collect(Collectors.toList());
        }

        // FAQ data holder
        private static class FAQ {
            String question;
            String answer;

            FAQ(String question, String answer) {
                this.question = question;
                this.answer = answer;
            }
        }

        // ---------- Porter Stemmer (simplified) ----------
        private static class Stemmer {
            private String stem(String word) {
                // Very basic stemmer: just remove common suffixes for demonstration
                // For production, use a full Porter stemmer, but this works for simple FAQ matching.
                String w = word.toLowerCase();
                if (w.length() <= 2) return w;
                if (w.endsWith("ing")) {
                    w = w.substring(0, w.length() - 3);
                    if (w.length() > 1 && !w.endsWith("e")) w += "e";
                } else if (w.endsWith("ed")) {
                    w = w.substring(0, w.length() - 2);
                } else if (w.endsWith("s") && !w.endsWith("ss")) {
                    w = w.substring(0, w.length() - 1);
                } else if (w.endsWith("ies")) {
                    w = w.substring(0, w.length() - 3) + "y";
                } else if (w.endsWith("es")) {
                    w = w.substring(0, w.length() - 2);
                }
                return w;
            }
        }
    }

    // -------- GUI (Swing) --------
    private static class ChatBotGUI {
        private final ChatBot bot;
        private JTextArea chatArea;
        private JTextField inputField;

        ChatBotGUI(ChatBot bot) {
            this.bot = bot;
            initUI();
        }

        private void initUI() {
            JFrame frame = new JFrame("Java ChatBot");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(500, 500);
            frame.setLayout(new BorderLayout());

            // Chat display area
            chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            chatArea.setWrapStyleWord(true);
            chatArea.setFont(new Font("Serif", Font.PLAIN, 14));
            JScrollPane scrollPane = new JScrollPane(chatArea);
            frame.add(scrollPane, BorderLayout.CENTER);

            // Input panel
            JPanel inputPanel = new JPanel(new BorderLayout());
            inputField = new JTextField();
            JButton sendButton = new JButton("Send");

            inputPanel.add(inputField, BorderLayout.CENTER);
            inputPanel.add(sendButton, BorderLayout.EAST);
            frame.add(inputPanel, BorderLayout.SOUTH);

            // Event handlers
            sendButton.addActionListener(e -> processInput());
            inputField.addActionListener(e -> processInput());

            // Welcome message
            appendMessage("Bot", "Hello! I am your FAQ assistant. Ask me anything about our services.");

            frame.setVisible(true);
        }

        private void processInput() {
            String userText = inputField.getText().trim();
            if (userText.isEmpty()) return;
            appendMessage("You", userText);
            inputField.setText("");

            // Get bot response (run in background to keep GUI responsive)
            SwingUtilities.invokeLater(() -> {
                String response = bot.getResponse(userText);
                appendMessage("Bot", response);
            });
        }

        private void appendMessage(String sender, String message) {
            chatArea.append(sender + ": " + message + "\n\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }

    // -------- Main Application --------
    public static void main(String[] args) {
        // Initialise ChatBot with some sample FAQs
        ChatBot bot = new ChatBot();

        // Training data: frequently asked questions
        List<String[]> trainingData = Arrays.asList(
                new String[]{"What are your operating hours?", "We are open 24/7, including holidays."},
                new String[]{"How can I contact support?", "You can email support@example.com or call +1-800-555-0199."},
                new String[]{"Where is your office located?", "Our main office is at 123 Main Street, New York, NY 10001."},
                new String[]{"Do you offer refunds?", "Yes, we offer a full refund within 30 days of purchase."},
                new String[]{"What payment methods do you accept?", "We accept credit cards, PayPal, and bank transfers."},
                new String[]{"How do I reset my password?", "Click on 'Forgot Password' on the login page and follow the instructions."},
                new String[]{"What is your return policy?", "You can return any product within 30 days for a full refund."},
                new String[]{"Can I track my order?", "Yes, once your order ships, we'll send you a tracking link via email."}
        );
        bot.train(trainingData);

        // Launch GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new ChatBotGUI(bot));
    }
}
