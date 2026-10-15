package com.mentivo.config;

import com.mentivo.entity.*;
import com.mentivo.entity.enums.*;
import com.mentivo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final CategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final TopicRepository topicRepository;
    private final MentorAvailabilityRepository availabilityRepository;
    private final ConnectionRepository connectionRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final PlatformSettingRepository settingRepository;
    private final MentorFollowerRepository mentorFollowerRepository;
    private final MentorSessionRepository mentorSessionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           MentorProfileRepository mentorProfileRepository,
                           CategoryRepository categoryRepository,
                           SkillRepository skillRepository,
                           TopicRepository topicRepository,
                           MentorAvailabilityRepository availabilityRepository,
                           ConnectionRepository connectionRepository,
                           ConversationRepository conversationRepository,
                           MessageRepository messageRepository,
                           BookingRepository bookingRepository,
                           ReviewRepository reviewRepository,
                           PlatformSettingRepository settingRepository,
                           MentorFollowerRepository mentorFollowerRepository,
                           MentorSessionRepository mentorSessionRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.categoryRepository = categoryRepository;
        this.skillRepository = skillRepository;
        this.topicRepository = topicRepository;
        this.availabilityRepository = availabilityRepository;
        this.connectionRepository = connectionRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.settingRepository = settingRepository;
        this.mentorFollowerRepository = mentorFollowerRepository;
        this.mentorSessionRepository = mentorSessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already initialized with seed data. Ensuring sample social media & follower data...");
            userRepository.findByEmail("priya.learner@example.com").ifPresent(learner -> {
                userRepository.findByEmail("rahul.sharma@example.com").ifPresent(user -> {
                    MentorProfile mp = user.getMentorProfile();
                    if (mp != null && mp.getYoutubeUrl() == null) {
                        mp.setYoutubeUrl("https://youtube.com/@rahulsharmatech");
                        mp.setShowYoutubeSubscribers(true);
                        mp.setYoutubeSubscribersCount(4200L);
                        mp.setYoutubeSubscribersSource("SELF_REPORTED");
                        mp.setInstagramUrl("https://instagram.com/rahul.codes");
                        mp.setShowInstagramFollowers(true);
                        mp.setInstagramFollowersCount(1850L);
                        mp.setInstagramFollowersSource("SELF_REPORTED");
                        mentorProfileRepository.save(mp);

                        if (!mentorFollowerRepository.existsByMentorIdAndLearnerId(mp.getId(), learner.getId())) {
                            mentorFollowerRepository.save(new MentorFollower(mp, learner));
                        }
                    }
                });
            });

            // Ensure sample completed bookings and reviews exist
            ensureSampleCompletedBookingsAndReviews();

            // Ensure mentors have direct categories populated
            ensureMentorCategories();

            // Seed sessions if none exist
            seedSampleSessions();

            // Synchronize accurate review counts and average ratings across all mentors from eligible completed session reviews
            syncMentorReviewStats();
            return;
        }

        log.info("Seeding database with realistic initial data for Mentivo...");

        // 1. Platform Settings
        settingRepository.save(new PlatformSetting("PLATFORM_COMMISSION_PERCENT", "20.0", "Platform revenue commission percentage"));
        settingRepository.save(new PlatformSetting("DEFAULT_CURRENCY", "INR", "Default system display currency"));

        // 2. Categories
        Category progCat = categoryRepository.save(new Category("Programming & Tech", "programming", "Code", "Software engineering, algorithms, web, and mobile development"));
        Category schoolCat = categoryRepository.save(new Category("School Education", "school-education", "GraduationCap", "CBSE, ICSE, state boards, Physics, Maths, and Chemistry"));
        Category langCat = categoryRepository.save(new Category("Languages", "languages", "Languages", "Spoken English, regional Indian languages, and foreign languages"));
        Category careerCat = categoryRepository.save(new Category("Career & Interview", "career", "Briefcase", "Resume review, mock interviews, leadership, and job transitions"));
        Category bizCat = categoryRepository.save(new Category("Business & Startup", "business", "TrendingUp", "Entrepreneurship, fundraising, product strategy, and growth"));
        Category designCat = categoryRepository.save(new Category("Design & Creative", "design", "Palette", "UI/UX design, Figma, illustration, and graphic arts"));
        Category musicCat = categoryRepository.save(new Category("Music & Arts", "music", "Music", "Acoustic guitar, keyboard, vocals, and sound production"));
        Category fitnessCat = categoryRepository.save(new Category("Fitness & Wellness", "fitness", "HeartPulse", "Home workouts, nutrition guidance, and yoga"));

        // 3. Topics & Skills
        Topic javaTopic = topicRepository.save(new Topic("Java", "java", progCat));
        Topic springTopic = topicRepository.save(new Topic("Spring Boot", "spring-boot", progCat));
        Topic reactTopic = topicRepository.save(new Topic("React", "react", progCat));
        Topic pythonTopic = topicRepository.save(new Topic("Python", "python", progCat));
        Topic dsaTopic = topicRepository.save(new Topic("DSA", "dsa", progCat));
        Topic sqlTopic = topicRepository.save(new Topic("SQL & Databases", "sql", progCat));

        Topic physicsTopic = topicRepository.save(new Topic("Physics", "physics", schoolCat));
        Topic mathTopic = topicRepository.save(new Topic("Mathematics", "mathematics", schoolCat));
        Topic chemTopic = topicRepository.save(new Topic("Chemistry", "chemistry", schoolCat));

        Topic englishTopic = topicRepository.save(new Topic("English Fluency", "english", langCat));
        Topic hindiTopic = topicRepository.save(new Topic("Hindi", "hindi", langCat));
        Topic kannadaTopic = topicRepository.save(new Topic("Kannada", "kannada", langCat));

        Topic resumeTopic = topicRepository.save(new Topic("Resume Review", "resume", careerCat));
        Topic interviewTopic = topicRepository.save(new Topic("Mock Technical Interview", "interview", careerCat));
        Topic startupTopic = topicRepository.save(new Topic("Startup & Pitch Deck", "startup", bizCat));
        Topic marketingTopic = topicRepository.save(new Topic("Growth Marketing", "marketing", bizCat));

        Topic uiuxTopic = topicRepository.save(new Topic("UI/UX Design", "ui-ux", designCat));
        Topic guitarTopic = topicRepository.save(new Topic("Guitar", "guitar", musicCat));
        Topic yogaTopic = topicRepository.save(new Topic("Yoga & Mobility", "yoga", fitnessCat));

        Skill sJava = skillRepository.save(new Skill("Java", progCat));
        Skill sSpring = skillRepository.save(new Skill("Spring Boot", progCat));
        Skill sReact = skillRepository.save(new Skill("React.js", progCat));
        Skill sPython = skillRepository.save(new Skill("Python", progCat));
        Skill sDsa = skillRepository.save(new Skill("Data Structures", progCat));
        Skill sSql = skillRepository.save(new Skill("MySQL", progCat));
        Skill sPhysics = skillRepository.save(new Skill("Physics", schoolCat));
        Skill sMaths = skillRepository.save(new Skill("Calculus", schoolCat));
        Skill sEnglish = skillRepository.save(new Skill("Spoken English", langCat));
        Skill sInterview = skillRepository.save(new Skill("System Design", careerCat));
        Skill sGuitar = skillRepository.save(new Skill("Acoustic Guitar", musicCat));
        Skill sFigma = skillRepository.save(new Skill("Figma", designCat));

        String defaultHashedPwd = passwordEncoder.encode("Password@123");

        // 4. Admin Account
        User admin = new User("admin@mentivo.com", defaultHashedPwd, "Mentivo Admin", PrimaryRole.ADMIN);
        admin.setRoles(Set.of(UserRoleType.ROLE_ADMIN, UserRoleType.ROLE_LEARNER));
        admin.setHeadline("Mentivo Platform Administrator");
        admin.setVerified(true);
        admin.setVerificationLevel(VerificationLevel.EXPERT_VERIFIED);
        userRepository.save(admin);

        // 5. Create Mentors (20+ realistic profiles)
        // User A: Rahul Sharma (Java & Spring Boot) - Male
        User rahul = createUser("rahul.sharma@example.com", "Rahul Sharma", "Senior Java & Spring Boot Architect",
                "10+ years building high-scale fintech backends. Passionate about teaching clean code, concurrency, and microservices.",
                "Bengaluru, India", "English, Hindi", PrimaryRole.BOTH, VerificationLevel.EXPERT_VERIFIED,
                "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=240&auto=format&fit=crop&q=80");
        createMentor(rahul, 300.0, 10, OnlineStatus.ONLINE, 4.9, 38, 120, 10,
                Set.of(sJava, sSpring, sDsa, sSql), Set.of(javaTopic, springTopic, dsaTopic, sqlTopic));

        // Mentor 2: Priya Kulkarni (Physics & Math) - Female
        User priya = createUser("priya.kulkarni@example.com", "Priya Kulkarni", "IIT Bombay Alum | Physics & Mathematics Educator",
                "Specialized in simplifying complex physics concepts and calculus for high school and entrance exams.",
                "Pune, India", "English, Marathi, Hindi", PrimaryRole.MENTOR, VerificationLevel.EXPERT_VERIFIED,
                "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=240&auto=format&fit=crop&q=80");
        createMentor(priya, 350.0, 7, OnlineStatus.ONLINE, 4.95, 45, 140, 15,
                Set.of(sPhysics, sMaths), Set.of(physicsTopic, mathTopic));

        // Mentor 3: Rohan Shah (Startup Founder & Tech Lead) - Male
        User rohan = createUser("rohan.shah@example.com", "Rohan Shah", "Ex-Y Combinator Founder | Full-Stack & Business Mentor",
                "Helping engineers transition to startup founders, build MVPs, and prepare pitch decks.",
                "Mumbai, India", "English, Gujarati, Hindi", PrimaryRole.BOTH, VerificationLevel.EXPERT_VERIFIED,
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=240&auto=format&fit=crop&q=80");
        createMentor(rohan, 600.0, 8, OnlineStatus.ONLINE, 4.85, 29, 90, 20,
                Set.of(sReact, sPython), Set.of(reactTopic, startupTopic, marketingTopic));

        // Mentor 4: Sneha Deshmukh (UI/UX Designer) - Female
        User sneha = createUser("sneha.deshmukh@example.com", "Sneha Deshmukh", "Lead Product Designer at HealthTech",
                "Mentoring aspiring designers on design systems, Figma workflows, and creating standout portfolios.",
                "Bengaluru, India", "English, Hindi", PrimaryRole.MENTOR, VerificationLevel.SKILL_VERIFIED,
                "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=240&auto=format&fit=crop&q=80");
        createMentor(sneha, 280.0, 6, OnlineStatus.ONLINE, 4.8, 22, 65, 15,
                Set.of(sFigma), Set.of(uiuxTopic));

        // Mentor 5: Aditya Joshi (DSA & Competitive Programming) - Male
        User aditya = createUser("aditya.joshi@example.com", "Aditya Joshi", "SDE II @ Google | ICPC Regionalist",
                "Cracked Google and Amazon interviews. Specialized in dynamic programming, graphs, and system design mock interviews.",
                "Hyderabad, India", "English, Hindi", PrimaryRole.BOTH, VerificationLevel.EXPERT_VERIFIED,
                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=240&auto=format&fit=crop&q=80");
        createMentor(aditya, 500.0, 5, OnlineStatus.OFFLINE, 4.9, 52, 175, 10,
                Set.of(sDsa, sJava, sInterview), Set.of(dsaTopic, interviewTopic));

        // Mentor 6: Ananya Rao (English & Business Communication) - Female (FREE MENTOR)
        User ananya = createUser("ananya.rao@example.com", "Ananya Rao", "Volunteer Communications Coach & IELTS 8.5 (Free Mentoring)",
                "Giving back to the community by helping students and early-career jobseekers gain confidence in spoken English and interview communication for free.",
                "Chennai, India", "English, Tamil", PrimaryRole.MENTOR, VerificationLevel.SKILL_VERIFIED,
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=240&auto=format&fit=crop&q=80");
        createMentor(ananya, 0.0, 9, OnlineStatus.ONLINE, 4.92, 34, 110, 15,
                Set.of(sEnglish), Set.of(englishTopic));

        // Mentor 7: Vivek Kumar (Python & AI/ML) - Male
        User vivek = createUser("vivek.kumar@example.com", "Vivek Kumar", "Data Scientist & Python Instructor",
                "Hands-on guidance in Python, pandas, machine learning foundations, and data-driven problem solving.",
                "Delhi, India", "English, Hindi", PrimaryRole.BOTH, VerificationLevel.SKILL_VERIFIED,
                "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=240&auto=format&fit=crop&q=80");
        createMentor(vivek, 320.0, 6, OnlineStatus.ONLINE, 4.85, 27, 85, 15,
                Set.of(sPython, sSql), Set.of(pythonTopic, sqlTopic));

        // Mentor 8: Aarav Sharma (Acoustic Guitar & Music) - Male (FREE MENTOR)
        User aarav = createUser("aarav.sharma@example.com", "Aarav Sharma", "Community Music Educator | Free Beginner Acoustic Guitar",
                "Passionate about making music accessible to everyone. Learn fundamental chords, tuning, and strumming patterns at zero cost.",
                "Jaipur, India", "English, Hindi", PrimaryRole.MENTOR, VerificationLevel.SKILL_VERIFIED,
                "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=240&auto=format&fit=crop&q=80");
        createMentor(aarav, 0.0, 8, OnlineStatus.ONLINE, 4.9, 41, 130, 20,
                Set.of(sGuitar), Set.of(guitarTopic));

        // Mentor 9: Meera Iyer (Chemistry & NEET Prep) - Female
        User meera = createUser("meera.iyer@example.com", "Meera Iyer", "Senior Chemistry Lecturer (12+ yrs exp)",
                "Physical, Organic, and Inorganic Chemistry tailored for CBSE board and entrance examinations.",
                "Bengaluru, India", "English, Kannada, Hindi", PrimaryRole.MENTOR, VerificationLevel.EXPERT_VERIFIED,
                "https://images.unsplash.com/photo-1567532939604-b6b5b0db2604?w=240&auto=format&fit=crop&q=80");
        createMentor(meera, 300.0, 12, OnlineStatus.ONLINE, 4.92, 58, 210, 15,
                Set.of(sPhysics), Set.of(chemTopic));

        // Mentor 10: Karan Malhotra (Resume & Career Pivot) - Male
        User karan = createUser("karan.malhotra@example.com", "Karan Malhotra", "Senior Tech Recruiter & Career Strategist",
                "Craft ATS-beating resumes, optimize your LinkedIn profile, and negotiate tech salaries.",
                "Gurugram, India", "English, Hindi", PrimaryRole.MENTOR, VerificationLevel.SKILL_VERIFIED,
                "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=240&auto=format&fit=crop&q=80");
        createMentor(karan, 400.0, 9, OnlineStatus.OFFLINE, 4.88, 36, 95, 25,
                Set.of(sInterview), Set.of(resumeTopic, interviewTopic));

        // Mentor 11: Tanvi Nair (Yoga & Wellness Coach) - Female
        User tanvi = createUser("tanvi.nair@example.com", "Tanvi Nair", "Certified Hatha Yoga & Breathwork Instructor",
                "Desk-job postural relief, breathing exercises for stress reduction, and mindful movement.",
                "Kochi, India", "English, Malayalam", PrimaryRole.MENTOR, VerificationLevel.SKILL_VERIFIED,
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=240&auto=format&fit=crop&q=80");
        createMentor(tanvi, 220.0, 5, OnlineStatus.ONLINE, 4.95, 19, 70, 15,
                Set.of(sGuitar), Set.of(yogaTopic));

        // Mentor 12: Vikram Sengupta (System Design & Distributed Systems) - Male
        User vikram = createUser("vikram.sengupta@example.com", "Vikram Sengupta", "Principal Engineer @ FinTech Unicorn",
                "Deep dives into high-throughput microservices, event-driven architectures, Kafka, and scalability.",
                "Kolkata, India", "English, Bengali, Hindi", PrimaryRole.BOTH, VerificationLevel.EXPERT_VERIFIED,
                "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=240&auto=format&fit=crop&q=80");
        createMentor(vikram, 750.0, 14, OnlineStatus.OFFLINE, 4.97, 62, 190, 10,
                Set.of(sJava, sSpring, sSql), Set.of(javaTopic, springTopic, sqlTopic));

        // Additional 8 Mentors across subjects with specific gender portraits and rates
        Object[][] additionalMentors = {
                {"siddharth.verma@example.com", "Siddharth Verma", "React & Frontend Architect", "Ahmedabad", 250.0, 4, 4.75, 21, "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=240&auto=format&fit=crop&q=80"},
                {"pooja.hegde@example.com", "Pooja Hegde", "Spoken Kannada Volunteer Tutor (Free Practice)", "Mysuru", 0.0, 6, 4.9, 18, "https://images.unsplash.com/photo-1594744803329-e58b31de8bf5?w=240&auto=format&fit=crop&q=80"},
                {"manish.tiwari@example.com", "Manish Tiwari", "Maths & Statistics Specialist", "Lucknow", 280.0, 8, 4.8, 25, "https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?w=240&auto=format&fit=crop&q=80"},
                {"divya.krishnan@example.com", "Divya Krishnan", "Digital Growth & SEO Consultant", "Coimbatore", 320.0, 5, 4.7, 16, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=240&auto=format&fit=crop&q=80"},
                {"arjun.nambiar@example.com", "Arjun Nambiar", "DevOps & Cloud Architect", "Thiruvananthapuram", 450.0, 9, 4.85, 30, "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=240&auto=format&fit=crop&q=80"},
                {"ritu.singh@example.com", "Ritu Singh", "Interview Mindset & Soft Skills Coach", "Chandigarh", 220.0, 7, 4.8, 22, "https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?w=240&auto=format&fit=crop&q=80"},
                {"ashok.patel@example.com", "Ashok Patel", "SQL Database Performance Tuner", "Surat", 350.0, 11, 4.9, 35, "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=240&auto=format&fit=crop&q=80"},
                {"neha.agarwal@example.com", "Neha Agarwal", "SAT/GRE Quantitative Reasoning Tutor", "Indore", 400.0, 6, 4.88, 28, "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=240&auto=format&fit=crop&q=80"}
        };

        for (Object[] m : additionalMentors) {
            User u = createUser((String) m[0], (String) m[1], (String) m[2],
                    "Passionate educator with years of industry and academic experience.",
                    (String) m[3] + ", India", "English, Hindi", PrimaryRole.MENTOR, VerificationLevel.SKILL_VERIFIED,
                    (String) m[8]);
            createMentor(u, (Double) m[4], (Integer) m[5], OnlineStatus.ONLINE,
                    (Double) m[6], (Integer) m[7], 60, 20,
                    Set.of(sJava, sReact), Set.of(javaTopic, reactTopic));
        }

        // 6. Create Dedicated Learners (User B, User C, etc.)
        User priyaLearner = createUser("priya.learner@example.com", "Priya Kulkarni (Learner)",
                "Aspiring Backend Developer | CS Undergrad", "Learning Spring Boot, databases, and preparing for tech placements.",
                "Pune, India", "English, Marathi, Hindi", PrimaryRole.LEARNER, VerificationLevel.IDENTITY_VERIFIED,
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150");

        User amit = createUser("amit.learner@example.com", "Amit Verma",
                "Frontend Developer transitioning to Full-Stack", "Eager to understand Spring Boot microservices and REST design.",
                "Noida, India", "English, Hindi", PrimaryRole.LEARNER, VerificationLevel.IDENTITY_VERIFIED, null);

        User deepa = createUser("deepa.student@example.com", "Deepa Sundaram",
                "Class 12 CBSE Student", "Focusing on Physics mechanics and entrance exam preparation.",
                "Chennai, India", "English, Tamil", PrimaryRole.LEARNER, VerificationLevel.IDENTITY_VERIFIED, null);

        // 7. Connections between sample users
        Connection c1 = connectionRepository.save(new Connection(priyaLearner, rahul, "Hi Rahul, I'd love to connect and learn Spring Boot pagination and JPA from you!"));
        c1.setStatus(ConnectionStatus.ACCEPTED);
        connectionRepository.save(c1);

        Connection c2 = connectionRepository.save(new Connection(amit, rahul, "Hello Rahul, looking for guidance on Spring Security architecture."));
        c2.setStatus(ConnectionStatus.ACCEPTED);
        connectionRepository.save(c2);

        Connection c3 = connectionRepository.save(new Connection(deepa, priya, "Hello ma'am, I have questions about electromagnetism!"));
        c3.setStatus(ConnectionStatus.PENDING);
        connectionRepository.save(c3);

        // 8. Sample Conversation & Messages
        Conversation conv = conversationRepository.save(new Conversation(priyaLearner, rahul));
        conv.setLastMessageAt(LocalDateTime.now().minusHours(1));
        conv.setLastMessagePreview("I need help understanding Spring Boot pagination.");
        conversationRepository.save(conv);

        messageRepository.save(new Message(conv, priyaLearner, rahul, "Hi Rahul, thanks for connecting!", MessageType.TEXT));
        messageRepository.save(new Message(conv, rahul, priyaLearner, "Hey Priya! Happy to connect. What topic are you working on?", MessageType.TEXT));
        messageRepository.save(new Message(conv, priyaLearner, rahul, "I need help understanding Spring Boot pagination.", MessageType.TEXT));
        messageRepository.save(new Message(conv, rahul, priyaLearner, "Sure! We can use Spring Data JPA Pageable and Page<T>. Let's book a 30-min quick session!", MessageType.TEXT));

        // 9. Sample Bookings
        MentorProfile rahulProfile = rahul.getMentorProfile();
        Booking completedBooking = new Booking();
        completedBooking.setBookingRef("MNT-DEMO001");
        completedBooking.setLearner(priyaLearner);
        completedBooking.setMentor(rahulProfile);
        completedBooking.setTopic(springTopic);
        completedBooking.setBookingDate(LocalDate.now().minusDays(2));
        completedBooking.setStartTime(LocalTime.of(18, 0));
        completedBooking.setEndTime(LocalTime.of(18, 30));
        completedBooking.setDurationMinutes(30);
        completedBooking.setHourlyRate(BigDecimal.valueOf(300.0));
        completedBooking.setSessionPrice(BigDecimal.valueOf(150.0));
        completedBooking.setPlatformFee(BigDecimal.valueOf(30.0));
        completedBooking.setTotalAmount(BigDecimal.valueOf(150.0));
        completedBooking.setMentorEarnings(BigDecimal.valueOf(120.0));
        completedBooking.setStatus(BookingStatus.COMPLETED);
        completedBooking.setMeetingUrl("https://meet.mentivo.com/room/MNT-DEMO001?token=sample123");
        completedBooking.setMeetingNotes("Spring Boot pagination walkthrough");
        bookingRepository.save(completedBooking);

        // 10. Sample Review for completed booking
        Review r1 = new Review(completedBooking, rahulProfile, priyaLearner, 5,
                "Rahul was phenomenal! Explained PageRequest, Pageable, and Slice in 30 minutes with crystal clarity. Highly recommended!");
        reviewRepository.save(r1);

        ensureSampleCompletedBookingsAndReviews();
        syncMentorReviewStats();

        log.info("Mentivo seed data initialized successfully! Total users: {}, Total mentors: {}",
                userRepository.count(), mentorProfileRepository.count());
    }

    private User createUser(String email, String name, String headline, String bio, String loc, String langs,
                            PrimaryRole role, VerificationLevel vLevel, String image) {
        User u = new User(email, passwordEncoder.encode("Password@123"), name, role);
        u.setHeadline(headline);
        u.setBio(bio);
        u.setLocation(loc);
        u.setLanguages(langs);
        u.setVerificationLevel(vLevel);
        u.setVerified(vLevel != VerificationLevel.UNVERIFIED);
        u.setProfileImage(image != null ? image : "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
        Set<UserRoleType> roles = new HashSet<>();
        roles.add(UserRoleType.ROLE_LEARNER);
        if (role == PrimaryRole.MENTOR || role == PrimaryRole.BOTH) {
            roles.add(UserRoleType.ROLE_MENTOR);
        }
        u.setRoles(roles);
        return userRepository.save(u);
    }

    private void createMentor(User user, Double rate, int exp, OnlineStatus status, Double rating, int reviews, int sessions,
                              int responseTime, Set<Skill> skills, Set<Topic> topics) {
        MentorProfile p = new MentorProfile(user, BigDecimal.valueOf(rate), exp);
        p.setOnlineStatus(status);
        p.setAverageRating(rating);
        p.setTotalReviews(reviews);
        p.setSessionsCompleted(sessions);
        p.setResponseTimeMinutes(responseTime);
        p.setBioSummary(user.getBio());
        p.setSkills(skills);
        p.setTopics(topics);

        Set<Category> cats = new HashSet<>();
        if (topics != null) {
            for (Topic t : topics) {
                if (t.getCategory() != null) cats.add(t.getCategory());
            }
        }
        if (skills != null) {
            for (Skill s : skills) {
                if (s.getCategory() != null) cats.add(s.getCategory());
            }
        }
        p.setCategories(cats);

        MentorProfile saved = mentorProfileRepository.save(p);
        user.setMentorProfile(saved);

        // Create standard availabilities
        List<MentorAvailability> avails = new ArrayList<>();
        avails.add(new MentorAvailability(saved, DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(22, 0)));
        avails.add(new MentorAvailability(saved, DayOfWeek.TUESDAY, LocalTime.of(18, 0), LocalTime.of(22, 0)));
        avails.add(new MentorAvailability(saved, DayOfWeek.WEDNESDAY, LocalTime.of(18, 0), LocalTime.of(22, 0)));
        avails.add(new MentorAvailability(saved, DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)));
        avails.add(new MentorAvailability(saved, DayOfWeek.SUNDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)));
        availabilityRepository.saveAll(avails);
    }

    private void ensureMentorCategories() {
        List<MentorProfile> allMentors = mentorProfileRepository.findAll();
        for (MentorProfile mp : allMentors) {
            if (mp.getCategories() == null || mp.getCategories().isEmpty()) {
                Set<Category> cats = new HashSet<>();
                if (mp.getTopics() != null) {
                    for (Topic t : mp.getTopics()) {
                        if (t.getCategory() != null) cats.add(t.getCategory());
                    }
                }
                if (mp.getSkills() != null) {
                    for (Skill s : mp.getSkills()) {
                        if (s.getCategory() != null) cats.add(s.getCategory());
                    }
                }
                if (!cats.isEmpty()) {
                    mp.setCategories(cats);
                    mentorProfileRepository.save(mp);
                }
            }
        }
    }

    private void seedSampleSessions() {
        if (mentorSessionRepository.count() > 0) return;

        List<MentorProfile> allMentors = mentorProfileRepository.findAll();
        if (allMentors.isEmpty()) return;

        log.info("Seeding realistic sample mentor sessions and webinars...");
        MentorProfile rahulProfile = allMentors.stream()
                .filter(m -> m.getUser() != null && "rahul.sharma@example.com".equals(m.getUser().getEmail()))
                .findFirst().orElse(allMentors.get(0));

        List<Category> allCategories = categoryRepository.findAll();
        Category fallbackCat = !allCategories.isEmpty() ? allCategories.get(0) : null;

        Category progCat = categoryRepository.findBySlug("programming").orElse(fallbackCat);
        Category careerCat = categoryRepository.findBySlug("career").orElse(fallbackCat);
        Category designCat = categoryRepository.findBySlug("design").orElse(fallbackCat);
        Category langCat = categoryRepository.findBySlug("languages").orElse(fallbackCat);

        Topic springTopic = topicRepository.findBySlug("spring-boot").orElse(null);
        Topic dsaTopic = topicRepository.findBySlug("dsa").orElse(null);
        Topic interviewTopic = topicRepository.findBySlug("interview").orElse(null);
        Topic uiuxTopic = topicRepository.findBySlug("ui-ux").orElse(null);
        Topic englishTopic = topicRepository.findBySlug("english").orElse(null);

        List<MentorSession> sessions = new ArrayList<>();

        // 1. Webinar - Free (Programming)
        MentorSession s1 = new MentorSession();
        s1.setMentor(rahulProfile);
        s1.setTitle("Spring Boot 3 & Cloud Architecture Masterclass");
        s1.setDescription("Deep dive into microservices, virtual threads, reactive endpoints, and production deployment best practices.");
        s1.setCategory(progCat);
        s1.setTopic(springTopic);
        s1.setSessionType(SessionType.WEBINAR);
        s1.setSessionDate(LocalDate.now().plusDays(2));
        s1.setStartTime(LocalTime.of(19, 0));
        s1.setEndTime(LocalTime.of(20, 30));
        s1.setDurationMinutes(90);
        s1.setIsFree(true);
        s1.setPrice(BigDecimal.ZERO);
        s1.setExternalMeetingUrl("https://meet.google.com/xyz-ment-ivo");
        s1.setStatus(SessionStatus.PUBLISHED);
        sessions.add(s1);

        // 2. Group Session - Paid (Programming)
        MentorSession s2 = new MentorSession();
        s2.setMentor(rahulProfile);
        s2.setTitle("High-Concurrency Backend Systems & Kafka Walkthrough");
        s2.setDescription("Hands-on architecture session dissecting message brokers, partitioned topics, and zero-data-loss pipelines.");
        s2.setCategory(progCat);
        s2.setTopic(springTopic);
        s2.setSessionType(SessionType.GROUP_SESSION);
        s2.setSessionDate(LocalDate.now().plusDays(4));
        s2.setStartTime(LocalTime.of(18, 0));
        s2.setEndTime(LocalTime.of(19, 15));
        s2.setDurationMinutes(75);
        s2.setIsFree(false);
        s2.setPrice(BigDecimal.valueOf(199.0));
        s2.setStatus(SessionStatus.PUBLISHED);
        sessions.add(s2);

        // Other mentors
        MentorProfile m2 = allMentors.size() > 1 ? allMentors.get(1) : rahulProfile;
        MentorProfile m3 = allMentors.size() > 2 ? allMentors.get(2) : rahulProfile;

        // 3. Individual Session - Paid
        MentorSession s3 = new MentorSession();
        s3.setMentor(m2);
        s3.setTitle("1-on-1 Data Structures & Algorithms Deep Dive");
        s3.setDescription("Personalized algorithmic problem solving, time complexity optimization, and pattern recognition for technical interviews.");
        s3.setCategory(progCat != null ? progCat : fallbackCat);
        s3.setTopic(dsaTopic);
        s3.setSessionType(SessionType.INDIVIDUAL_SESSION);
        s3.setSessionDate(LocalDate.now().plusDays(3));
        s3.setStartTime(LocalTime.of(16, 0));
        s3.setEndTime(LocalTime.of(17, 0));
        s3.setDurationMinutes(60);
        s3.setIsFree(false);
        s3.setPrice(BigDecimal.valueOf(250.0));
        s3.setStatus(SessionStatus.PUBLISHED);
        sessions.add(s3);

        // 4. Webinar - Free (Career Guidance)
        MentorSession s4 = new MentorSession();
        s4.setMentor(m2);
        s4.setTitle("Cracking Product Company Tech Interviews: The Exact Playbook");
        s4.setDescription("How to prepare system design, behavioral questions, resume screening secrets, and offer negotiation.");
        s4.setCategory(careerCat != null ? careerCat : progCat);
        s4.setTopic(interviewTopic);
        s4.setSessionType(SessionType.WEBINAR);
        s4.setSessionDate(LocalDate.now().plusDays(5));
        s4.setStartTime(LocalTime.of(20, 0));
        s4.setEndTime(LocalTime.of(21, 0));
        s4.setDurationMinutes(60);
        s4.setIsFree(true);
        s4.setPrice(BigDecimal.ZERO);
        s4.setExternalMeetingUrl("https://meet.google.com/abc-tech-play");
        s4.setStatus(SessionStatus.PUBLISHED);
        sessions.add(s4);

        // 5. Group Session - Free (Design)
        if (designCat != null) {
            MentorSession s5 = new MentorSession();
            s5.setMentor(m3);
            s5.setTitle("Design Systems from Scratch: Components, Tokens, and Figma");
            s5.setDescription("Learn modern UI design systems, auto-layout variants, and handoff workflows for developers.");
            s5.setCategory(designCat);
            s5.setTopic(uiuxTopic);
            s5.setSessionType(SessionType.GROUP_SESSION);
            s5.setSessionDate(LocalDate.now().plusDays(6));
            s5.setStartTime(LocalTime.of(17, 30));
            s5.setEndTime(LocalTime.of(18, 45));
            s5.setDurationMinutes(75);
            s5.setIsFree(true);
            s5.setPrice(BigDecimal.ZERO);
            s5.setStatus(SessionStatus.PUBLISHED);
            sessions.add(s5);
        }

        // 6. Individual Session - Paid (Languages)
        if (langCat != null) {
            MentorSession s6 = new MentorSession();
            s6.setMentor(m3);
            s6.setTitle("Executive English & Global Workplace Communication");
            s6.setDescription("Practice boardroom presentations, conversational confidence, and cross-cultural business communication.");
            s6.setCategory(langCat);
            s6.setTopic(englishTopic);
            s6.setSessionType(SessionType.INDIVIDUAL_SESSION);
            s6.setSessionDate(LocalDate.now().plusDays(7));
            s6.setStartTime(LocalTime.of(15, 0));
            s6.setEndTime(LocalTime.of(15, 45));
            s6.setDurationMinutes(45);
            s6.setIsFree(false);
            s6.setPrice(BigDecimal.valueOf(180.0));
            s6.setStatus(SessionStatus.PUBLISHED);
            sessions.add(s6);
        }

        mentorSessionRepository.saveAll(sessions);
        log.info("Successfully seeded {} mentor sessions and webinars.", sessions.size());
    }

    private void ensureSampleCompletedBookingsAndReviews() {
        Optional<User> deepaOpt = userRepository.findByEmail("deepa.student@example.com");
        Optional<User> priyaLearnerOpt = userRepository.findByEmail("priya.learner@example.com");
        Optional<User> amitOpt = userRepository.findByEmail("amit.learner@example.com");

        // 1. Rahul Sharma secondary review
        userRepository.findByEmail("rahul.sharma@example.com").ifPresent(rahul -> {
            MentorProfile rahulProfile = rahul.getMentorProfile();
            if (rahulProfile != null && deepaOpt.isPresent() && bookingRepository.findByBookingRef("MNT-DEMO002").isEmpty()) {
                Booking b2 = createCompletedBooking("MNT-DEMO002", deepaOpt.get(), rahulProfile,
                        topicRepository.findBySlug("java").orElse(null), 45, 300.0,
                        "Java Generics, Streams & Collections deep dive");
                bookingRepository.save(b2);
                Review r2 = new Review(b2, rahulProfile, deepaOpt.get(), 5,
                        "Outstanding session on Java Generics and Streams. Rahul gave practical enterprise examples that immediately cleared up my doubts!");
                reviewRepository.save(r2);
                log.info("Seeded secondary completed booking MNT-DEMO002 and verified review for Rahul Sharma.");
            }
        });

        // 2. Priya Kulkarni (Physics & Math Educator)
        userRepository.findByEmail("priya.kulkarni@example.com").ifPresent(priya -> {
            MentorProfile priyaProfile = priya.getMentorProfile();
            if (priyaProfile != null && deepaOpt.isPresent() && bookingRepository.findByBookingRef("MNT-DEMO003").isEmpty()) {
                Booking b3 = createCompletedBooking("MNT-DEMO003", deepaOpt.get(), priyaProfile,
                        topicRepository.findBySlug("physics").orElse(null), 60, 350.0,
                        "Newtonian Mechanics and Electrostatics problem solving");
                bookingRepository.save(b3);
                Review r3 = new Review(b3, priyaProfile, deepaOpt.get(), 5,
                        "Priya ma'am makes Physics mechanics feel so intuitive! Her step-by-step problem-solving shortcuts and patience are unmatched.");
                reviewRepository.save(r3);
                log.info("Seeded completed booking MNT-DEMO003 and verified review for Priya Kulkarni.");
            }
        });

        // 3. Rohan Shah (Startup Founder & Full-Stack)
        userRepository.findByEmail("rohan.shah@example.com").ifPresent(rohan -> {
            MentorProfile rohanProfile = rohan.getMentorProfile();
            if (rohanProfile != null && amitOpt.isPresent() && bookingRepository.findByBookingRef("MNT-DEMO004").isEmpty()) {
                Booking b4 = createCompletedBooking("MNT-DEMO004", amitOpt.get(), rohanProfile,
                        topicRepository.findBySlug("startup").orElse(null), 45, 600.0,
                        "MVP technical roadmap and investor pitch review");
                bookingRepository.save(b4);
                Review r4 = new Review(b4, rohanProfile, amitOpt.get(), 5,
                        "Rohan gave incredible tactical feedback on my MVP architecture and fundraising pitch deck. Highly actionable and inspiring session!");
                reviewRepository.save(r4);
                log.info("Seeded completed booking MNT-DEMO004 and verified review for Rohan Shah.");
            }
        });

        // 4. Sneha Deshmukh (UI/UX Design)
        userRepository.findByEmail("sneha.deshmukh@example.com").ifPresent(sneha -> {
            MentorProfile snehaProfile = sneha.getMentorProfile();
            if (snehaProfile != null && priyaLearnerOpt.isPresent() && bookingRepository.findByBookingRef("MNT-DEMO005").isEmpty()) {
                Booking b5 = createCompletedBooking("MNT-DEMO005", priyaLearnerOpt.get(), snehaProfile,
                        topicRepository.findBySlug("ui-ux").orElse(null), 45, 280.0,
                        "Design systems, auto-layout, and portfolio critique");
                bookingRepository.save(b5);
                Review r5 = new Review(b5, snehaProfile, priyaLearnerOpt.get(), 5,
                        "Sneha reviewed my Figma portfolio and helped me refine my design system tokens. Invaluable guidance for landing UX interviews!");
                reviewRepository.save(r5);
                log.info("Seeded completed booking MNT-DEMO005 and verified review for Sneha Deshmukh.");
            }
        });

        // 5. Ananya Rao (English Communication)
        userRepository.findByEmail("ananya.rao@example.com").ifPresent(ananya -> {
            MentorProfile ananyaProfile = ananya.getMentorProfile();
            if (ananyaProfile != null && amitOpt.isPresent() && bookingRepository.findByBookingRef("MNT-DEMO006").isEmpty()) {
                Booking b6 = createCompletedBooking("MNT-DEMO006", amitOpt.get(), ananyaProfile,
                        topicRepository.findBySlug("english").orElse(null), 30, 0.0,
                        "Mock behavioral interview & conversational fluency");
                bookingRepository.save(b6);
                Review r6 = new Review(b6, ananyaProfile, amitOpt.get(), 5,
                        "Ananya's mock behavioral interview gave me the exact phrasing and confidence I needed. So generous of her to offer free community mentoring!");
                reviewRepository.save(r6);
                log.info("Seeded completed booking MNT-DEMO006 and verified review for Ananya Rao.");
            }
        });
    }

    private Booking createCompletedBooking(String ref, User learner, MentorProfile mentor, Topic topic,
                                          int durationMinutes, double hourlyRate, String notes) {
        Booking b = new Booking();
        b.setBookingRef(ref);
        b.setLearner(learner);
        b.setMentor(mentor);
        b.setTopic(topic);
        b.setBookingDate(LocalDate.now().minusDays(2));
        b.setStartTime(LocalTime.of(18, 0));
        b.setEndTime(LocalTime.of(18, 0).plusMinutes(durationMinutes));
        b.setDurationMinutes(durationMinutes);
        b.setHourlyRate(BigDecimal.valueOf(hourlyRate));
        BigDecimal sessionPrice = BigDecimal.valueOf(hourlyRate * durationMinutes / 60.0).setScale(2, RoundingMode.HALF_UP);
        b.setSessionPrice(sessionPrice);
        b.setPlatformFee(BigDecimal.valueOf(sessionPrice.doubleValue() * 0.20).setScale(2, RoundingMode.HALF_UP));
        b.setTotalAmount(sessionPrice);
        b.setMentorEarnings(BigDecimal.valueOf(sessionPrice.doubleValue() * 0.80).setScale(2, RoundingMode.HALF_UP));
        b.setStatus(BookingStatus.COMPLETED);
        b.setMeetingUrl("https://meet.mentivo.com/room/" + ref + "?token=sampleKey");
        b.setMeetingNotes(notes);
        return b;
    }

    private void syncMentorReviewStats() {
        for (MentorProfile mp : mentorProfileRepository.findAll()) {
            Double avg = reviewRepository.calculateAverageRatingForMentor(mp.getId());
            long count = reviewRepository.countByMentorId(mp.getId());
            mp.setTotalReviews((int) count);
            mp.setAverageRating(avg != null ? BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0.0);
            mentorProfileRepository.save(mp);
        }
        log.info("Synchronized mentor review stats with eligible completed session reviews.");
    }
}
