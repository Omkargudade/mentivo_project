package com.mentivo.service;

import com.mentivo.dto.admin.AdminStatsDto;
import com.mentivo.dto.admin.SettingUpdateDto;
import com.mentivo.dto.admin.VerificationReviewRequest;
import com.mentivo.dto.booking.BookingDto;
import com.mentivo.dto.report.ReportDto;
import com.mentivo.dto.user.UserProfileDto;
import com.mentivo.entity.*;
import com.mentivo.entity.enums.*;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.repository.*;
import com.mentivo.util.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final BookingRepository bookingRepository;
    private final ReportRepository reportRepository;
    private final VerificationRequestRepository verificationRepository;
    private final PlatformSettingRepository settingRepository;
    private final NotificationService notificationService;

    public AdminService(UserRepository userRepository,
                        MentorProfileRepository mentorProfileRepository,
                        BookingRepository bookingRepository,
                        ReportRepository reportRepository,
                        VerificationRequestRepository verificationRepository,
                        PlatformSettingRepository settingRepository,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.bookingRepository = bookingRepository;
        this.reportRepository = reportRepository;
        this.verificationRepository = verificationRepository;
        this.settingRepository = settingRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public AdminStatsDto getAdminStats() {
        AdminStatsDto stats = new AdminStatsDto();
        stats.setTotalUsers(userRepository.count());
        stats.setActiveUsers(userRepository.countByIsActive(true));
        stats.setTotalMentors(mentorProfileRepository.count());
        stats.setTotalLearners(userRepository.countByPrimaryRole(PrimaryRole.LEARNER));
        stats.setOnlineMentors(mentorProfileRepository.countByOnlineStatus(OnlineStatus.ONLINE));
        stats.setTotalBookings(bookingRepository.count());
        stats.setCompletedSessions(bookingRepository.countByStatus(BookingStatus.COMPLETED));
        stats.setTotalRevenue(bookingRepository.calculateTotalRevenue());
        stats.setTotalPlatformFees(bookingRepository.calculateTotalPlatformFees());
        stats.setPendingReports(reportRepository.countByStatus(ReportStatus.PENDING));
        stats.setPendingVerifications(verificationRepository.countByStatus(VerificationStatus.PENDING));
        return stats;
    }

    @Transactional(readOnly = true)
    public Page<UserProfileDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(DtoMapper::toUserProfileDto);
    }

    @Transactional
    public UserProfileDto toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        user.setActive(!user.isActive());
        User saved = userRepository.save(user);
        return DtoMapper.toUserProfileDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReportDto> getReports(ReportStatus status, Pageable pageable) {
        if (status != null) {
            return reportRepository.findByStatus(status, pageable).map(DtoMapper::toReportDto);
        }
        return reportRepository.findAll(pageable).map(DtoMapper::toReportDto);
    }

    @Transactional
    public ReportDto resolveReport(Long reportId, String adminNotes, ReportStatus status) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));
        report.setAdminNotes(adminNotes);
        report.setStatus(status != null ? status : ReportStatus.ACTIONED);
        Report saved = reportRepository.save(report);
        return DtoMapper.toReportDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<BookingDto> getAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable).map(DtoMapper::toBookingDto);
    }

    @Transactional(readOnly = true)
    public List<PlatformSetting> getPlatformSettings() {
        return settingRepository.findAll();
    }

    @Transactional
    public PlatformSetting updatePlatformSetting(SettingUpdateDto dto) {
        PlatformSetting setting = settingRepository.findById(dto.getSettingKey())
                .orElse(new PlatformSetting(dto.getSettingKey(), dto.getSettingValue(), dto.getDescription()));
        setting.setSettingValue(dto.getSettingValue());
        if (dto.getDescription() != null) setting.setDescription(dto.getDescription());
        return settingRepository.save(setting);
    }
}
