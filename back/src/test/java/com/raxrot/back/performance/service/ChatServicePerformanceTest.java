package com.raxrot.back.performance.service;

import com.raxrot.back.dtos.DialogDto;
import com.raxrot.back.dtos.SendMessageRequest;
import com.raxrot.back.models.Message;
import com.raxrot.back.models.User;
import com.raxrot.back.repositories.MessageRepository;
import com.raxrot.back.repositories.UserRepository;
import com.raxrot.back.services.impl.ChatServiceImpl;
import com.raxrot.back.utils.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatServicePerformanceTest {

    @Autowired
    private ChatServiceImpl chatService;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MessageRepository messageRepository;

    private ExecutorService executorService;
    private List<User> testUsers;
    private List<Message> testMessages;
    private User currentUser;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(25);

        // Создаем тестовых пользователей
        currentUser = createUser(1L, "current_user", "current@test.com");
        testUsers = Arrays.asList(
                currentUser,
                createUser(2L, "alice_chat", "alice@test.com"),
                createUser(3L, "bob_chat", "bob@test.com"),
                createUser(4L, "charlie_chat", "charlie@test.com"),
                createUser(5L, "diana_chat", "diana@test.com")
        );

        // Создаем тестовые сообщения (большой набор данных)
        testMessages = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(30);

        // Генерируем 1000+ сообщений для тестов
        long messageId = 1L;
        for (int day = 0; day < 30; day++) {
            for (int hour = 0; hour < 24; hour += 3) { // Каждые 3 часа
                for (User user : testUsers) {
                    if (!user.equals(currentUser)) {
                        // Создаем сообщения в обе стороны
                        testMessages.add(createMessage(
                                messageId++,
                                currentUser,
                                user,
                                "Hello from current user day " + day,
                                baseTime.plusDays(day).plusHours(hour)
                        ));
                        testMessages.add(createMessage(
                                messageId++,
                                user,
                                currentUser,
                                "Reply from " + user.getUserName() + " day " + day,
                                baseTime.plusDays(day).plusHours(hour + 1)
                        ));
                    }
                }
            }
        }

        // Мокаем AuthUtil
        when(authUtil.loggedInUserId()).thenReturn(currentUser.getUserId());
        when(authUtil.loggedInUser()).thenReturn(currentUser);

        // Мокаем UserRepository
        when(userRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            return testUsers.stream()
                    .filter(user -> user.getUserId().equals(userId))
                    .findFirst();
        });

        setupMessageRepositoryMocks();
    }

    private User createUser(Long id, String username, String email) {
        User user = new User();
        user.setUserId(id);
        user.setUserName(username);
        user.setEmail(email);
        user.setPassword("password");
        user.setProfilePic("https://test.com/profile" + id + ".jpg");
        return user;
    }

    private Message createMessage(Long id, User sender, User recipient, String text, LocalDateTime createdAt) {
        Message message = new Message();
        message.setId(id);
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setText(text);
        message.setCreatedAt(createdAt);
        // Каждое 5-е сообщение не прочитано
        if (id % 5 == 0) {
            message.setReadAt(null);
        } else {
            message.setReadAt(createdAt.plusMinutes(5));
        }
        return message;
    }

    private void setupMessageRepositoryMocks() {
        // Mock findConversation - возвращаем пагинированные сообщения
        when(messageRepository.findConversation(anyLong(), anyLong(), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Long meId = invocation.getArgument(0);
                    Long peerId = invocation.getArgument(1);
                    Pageable pageable = invocation.getArgument(2);

                    List<Message> conversation = testMessages.stream()
                            .filter(m -> (m.getSender().getUserId().equals(meId) && m.getRecipient().getUserId().equals(peerId)) ||
                                    (m.getSender().getUserId().equals(peerId) && m.getRecipient().getUserId().equals(meId)))
                            .sorted(Comparator.comparing(Message::getCreatedAt).thenComparing(Message::getId))
                            .collect(Collectors.toList());

                    int start = (int) pageable.getOffset();
                    int end = Math.min((start + pageable.getPageSize()), conversation.size());

                    return new PageImpl<>(
                            conversation.subList(start, end),
                            pageable,
                            conversation.size()
                    );
                });

        // Mock findNewAfter - возвращаем сообщения после определенного ID
        when(messageRepository.findNewAfter(anyLong(), anyLong(), anyLong()))
                .thenAnswer(invocation -> {
                    Long meId = invocation.getArgument(0);
                    Long peerId = invocation.getArgument(1);
                    Long afterId = invocation.getArgument(2);

                    return testMessages.stream()
                            .filter(m -> ((m.getSender().getUserId().equals(meId) && m.getRecipient().getUserId().equals(peerId)) ||
                                    (m.getSender().getUserId().equals(peerId) && m.getRecipient().getUserId().equals(meId))) &&
                                    m.getId() > afterId)
                            .sorted(Comparator.comparing(Message::getCreatedAt).thenComparing(Message::getId))
                            .collect(Collectors.toList());
                });

        // Mock findBySender_UserIdOrRecipient_UserIdOrderByCreatedAtDesc - для диалогов
        when(messageRepository.findBySender_UserIdOrRecipient_UserIdOrderByCreatedAtDesc(
                anyLong(), anyLong(), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Long senderId = invocation.getArgument(0);
                    Long recipientId = invocation.getArgument(1);
                    Pageable pageable = invocation.getArgument(2);

                    List<Message> userMessages = testMessages.stream()
                            .filter(m -> m.getSender().getUserId().equals(senderId) ||
                                    m.getRecipient().getUserId().equals(recipientId))
                            .sorted(Comparator.comparing(Message::getCreatedAt).reversed())
                            .limit(pageable.getPageSize())
                            .collect(Collectors.toList());

                    return new PageImpl<>(userMessages, pageable, userMessages.size());
                });

        // Mock save для отправки сообщений
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(9999L); // Присваиваем ID
            return message;
        });
    }

    @Test
    @DisplayName("💬 PERFORMANCE TEST: getConversation - Large History Pagination")
    void performanceTest_getConversation_LargeHistory() throws InterruptedException {
        int numberOfRequests = 500;
        Long peerId = 2L; // Alice
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("💬 getConversation Performance Test");

        System.out.println("\n" + "═".repeat(70));
        System.out.println("💬 PERFORMANCE TEST - getConversation (Large History)");
        System.out.println("═".repeat(70));

        stopWatch.start(numberOfRequests + " Conversation Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    int page = i % 5; // 5 разных страниц
                    int size = 20; // По 20 сообщений на страницу
                    chatService.getConversation(peerId, page, size);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(15, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("📊 TEST RESULTS:");
        System.out.println("═".repeat(70));
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("💫 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response Time: " +
                String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(70) + "\n");
    }

    @Test
    @DisplayName("🔄 PERFORMANCE TEST: getMyDialogs - Dialog Aggregation")
    void performanceTest_getMyDialogs_Aggregation() throws InterruptedException {
        int numberOfRequests = 300;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("🔄 getMyDialogs Performance Test");

        System.out.println("\n" + "═".repeat(70));
        System.out.println("🔄 PERFORMANCE TEST - getMyDialogs (Dialog Aggregation)");
        System.out.println("═".repeat(70));

        stopWatch.start(numberOfRequests + " Dialog Aggregation Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    List<DialogDto> dialogs = chatService.getMyDialogs();
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(10, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("📊 TEST RESULTS:");
        System.out.println("═".repeat(70));
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("💫 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response Time: " +
                String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(70) + "\n");
    }

    @Test
    @DisplayName("📨 PERFORMANCE TEST: sendMessage - Concurrent Messaging")
    void performanceTest_sendMessage_Concurrent() throws InterruptedException {
        int numberOfRequests = 400;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successfulSends = new AtomicInteger(0);
        StopWatch stopWatch = new StopWatch("📨 sendMessage Performance Test");

        System.out.println("\n" + "═".repeat(70));
        System.out.println("📨 PERFORMANCE TEST - sendMessage (Concurrent Messaging)");
        System.out.println("═".repeat(70));

        stopWatch.start(numberOfRequests + " Concurrent Message Sends");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    Long peerId = testUsers.get((i % 4) + 1).getUserId(); // Распределяем по разным пользователям
                    SendMessageRequest request = new SendMessageRequest();
                    request.setText("Test message " + i + " at " + System.currentTimeMillis());

                    chatService.send(peerId, request);
                    successfulSends.incrementAndGet();
                } catch (Exception e) {
                    // Игнорируем ошибки для теста производительности
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(20, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("📊 TEST RESULTS:");
        System.out.println("═".repeat(70));
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("✅ Successful Sends: " + successfulSends.get());
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("💫 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response Time: " +
                String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("📊 Success Rate: " +
                String.format("%.2f%%", (successfulSends.get() * 100.0 / numberOfRequests)));
        System.out.println("═".repeat(70) + "\n");
    }

    @Test
    @DisplayName("👁️ PERFORMANCE TEST: markRead - Bulk Read Updates")
    void performanceTest_markRead_BulkUpdates() throws InterruptedException {
        int numberOfRequests = 200;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("👁️ markRead Performance Test");

        System.out.println("\n" + "═".repeat(70));
        System.out.println("👁️ PERFORMANCE TEST - markRead (Bulk Read Updates)");
        System.out.println("═".repeat(70));

        stopWatch.start(numberOfRequests + " Mark Read Operations");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    Long peerId = testUsers.get((i % 4) + 1).getUserId();
                    chatService.markRead(peerId);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(10, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("📊 TEST RESULTS:");
        System.out.println("═".repeat(70));
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("💫 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response Time: " +
                String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(70) + "\n");
    }

    @Test
    @DisplayName("🆕 PERFORMANCE TEST: getNew - Real-time Updates")
    void performanceTest_getNew_RealTime() throws InterruptedException {
        int numberOfRequests = 600;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        StopWatch stopWatch = new StopWatch("🆕 getNew Performance Test");

        System.out.println("\n" + "═".repeat(70));
        System.out.println("🆕 PERFORMANCE TEST - getNew (Real-time Updates)");
        System.out.println("═".repeat(70));

        stopWatch.start(numberOfRequests + " Get New Messages Requests");

        IntStream.range(0, numberOfRequests).forEach(i -> {
            CompletableFuture.runAsync(() -> {
                try {
                    Long peerId = testUsers.get((i % 4) + 1).getUserId();
                    Long afterId = (long) (i * 10); // Разные afterId
                    chatService.getNew(peerId, afterId);
                } finally {
                    latch.countDown();
                }
            }, executorService);
        });

        latch.await(12, TimeUnit.SECONDS);
        stopWatch.stop();

        System.out.println("📊 TEST RESULTS:");
        System.out.println("═".repeat(70));
        System.out.println("📈 Total Requests: " + numberOfRequests);
        System.out.println("⏱️  Total Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("💫 RPS: " + String.format("%.2f", numberOfRequests * 1000.0 / stopWatch.getTotalTimeMillis()));
        System.out.println("📉 Avg Response Time: " +
                String.format("%.2f", stopWatch.getTotalTimeMillis() / (double) numberOfRequests) + " ms");
        System.out.println("═".repeat(70) + "\n");
    }

    @Test
    @DisplayName("💥 STRESS TEST: Mixed Chat Operations - 30 Second Load")
    void stressTest_MixedChatOperations() throws InterruptedException {
        int threads = 10;
        int durationSeconds = 30;
        AtomicInteger conversationCount = new AtomicInteger(0);
        AtomicInteger dialogsCount = new AtomicInteger(0);
        AtomicInteger sendCount = new AtomicInteger(0);
        AtomicInteger markReadCount = new AtomicInteger(0);
        AtomicInteger getNewCount = new AtomicInteger(0);

        StopWatch stopWatch = new StopWatch("💥 Mixed Chat Operations Stress Test");
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

        System.out.println("\n" + "═".repeat(70));
        System.out.println("💥 STRESS TEST - Mixed Chat Operations (30 Second Load)");
        System.out.println("═".repeat(70));

        stopWatch.start("Sustained Load - " + durationSeconds + " seconds");

        CompletableFuture<Void>[] workers = IntStream.range(0, threads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    Random random = new Random();
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            int operation = random.nextInt(100);
                            Long peerId = testUsers.get(random.nextInt(4) + 1).getUserId();

                            if (operation < 30) {
                                // 30% - получение диалогов
                                chatService.getMyDialogs();
                                dialogsCount.incrementAndGet();
                            } else if (operation < 55) {
                                // 25% - получение истории
                                chatService.getConversation(peerId, random.nextInt(3), 20);
                                conversationCount.incrementAndGet();
                            } else if (operation < 75) {
                                // 20% - отправка сообщений
                                SendMessageRequest request = new SendMessageRequest();
                                request.setText("Stress test message " + System.currentTimeMillis());
                                chatService.send(peerId, request);
                                sendCount.incrementAndGet();
                            } else if (operation < 90) {
                                // 15% - пометка прочитанными
                                chatService.markRead(peerId);
                                markReadCount.incrementAndGet();
                            } else {
                                // 10% - получение новых сообщений
                                chatService.getNew(peerId, (long) random.nextInt(1000));
                                getNewCount.incrementAndGet();
                            }

                            Thread.sleep(50 + random.nextInt(100)); // Имитация реальной нагрузки
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            // Игнорируем ошибки для стресс-теста
                        }
                    }
                }, executorService))
                .toArray(CompletableFuture[]::new);

        try {
            CompletableFuture.allOf(workers).get(35, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException e) {
            System.out.println("Stress test completed");
        }
        stopWatch.stop();

        int totalOperations = conversationCount.get() + dialogsCount.get() + sendCount.get() +
                markReadCount.get() + getNewCount.get();

        System.out.println("📊 STRESS TEST RESULTS:");
        System.out.println("═".repeat(70));
        System.out.println("⏱️  Test Duration: " + durationSeconds + " seconds");
        System.out.println("👥 Threads: " + threads);
        System.out.println("💬 Conversation Operations: " + conversationCount.get());
        System.out.println("🔄 Dialog Operations: " + dialogsCount.get());
        System.out.println("📨 Send Operations: " + sendCount.get());
        System.out.println("👁️ Mark Read Operations: " + markReadCount.get());
        System.out.println("🆕 Get New Operations: " + getNewCount.get());
        System.out.println("📈 Total Operations: " + totalOperations);
        System.out.println("🔥 Total OPS: " + String.format("%.2f", totalOperations / (double) durationSeconds));
        System.out.println("📊 Operation Distribution:");
        System.out.println("  • Conversations: " + String.format("%.1f%%", conversationCount.get() * 100.0 / totalOperations));
        System.out.println("  • Dialogs: " + String.format("%.1f%%", dialogsCount.get() * 100.0 / totalOperations));
        System.out.println("  • Sends: " + String.format("%.1f%%", sendCount.get() * 100.0 / totalOperations));
        System.out.println("  • Mark Read: " + String.format("%.1f%%", markReadCount.get() * 100.0 / totalOperations));
        System.out.println("  • Get New: " + String.format("%.1f%%", getNewCount.get() * 100.0 / totalOperations));
        System.out.println("═".repeat(70) + "\n");
    }

    @Test
    @DisplayName("📊 MEMORY TEST: Chat Operations Memory Usage")
    void memoryTest_ChatOperationsMemoryUsage() {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("📊 MEMORY TEST - Chat Operations Memory Usage");
        System.out.println("═".repeat(70));

        // Тестируем память для getMyDialogs (самой тяжелой операции)
        System.gc();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start("getMyDialogs Memory Usage");

        List<DialogDto> dialogs = chatService.getMyDialogs();

        stopWatch.stop();

        System.gc();
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        System.out.println("📊 MEMORY USAGE RESULTS:");
        System.out.println("═".repeat(70));
        System.out.println("💾 Memory Before: " + String.format("%.2f", memoryBefore / 1024.0 / 1024.0) + " MB");
        System.out.println("💾 Memory After: " + String.format("%.2f", memoryAfter / 1024.0 / 1024.0) + " MB");
        System.out.println("📈 Memory Used: " + String.format("%.2f", memoryUsed / 1024.0 / 1024.0) + " MB");
        System.out.println("⏱️  Operation Time: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("📋 Dialogs Returned: " + dialogs.size());
        System.out.println("⚡ Memory/Time Ratio: " +
                String.format("%.2f", memoryUsed / 1024.0 / stopWatch.getTotalTimeMillis()) + " KB/ms");
        System.out.println("═".repeat(70) + "\n");
    }
}