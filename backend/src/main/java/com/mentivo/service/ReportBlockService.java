package com.mentivo.service;

import com.mentivo.dto.report.BlockDto;
import com.mentivo.dto.report.BlockRequest;
import com.mentivo.dto.report.CreateReportRequest;
import com.mentivo.dto.report.ReportDto;
import com.mentivo.entity.Block;
import com.mentivo.entity.Report;
import com.mentivo.entity.User;
import com.mentivo.exception.BadRequestException;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.repository.BlockRepository;
import com.mentivo.repository.ReportRepository;
import com.mentivo.repository.UserRepository;
import com.mentivo.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ReportBlockService {

    private final ReportRepository reportRepository;
    private final BlockRepository blockRepository;
    private final UserRepository userRepository;

    public ReportBlockService(ReportRepository reportRepository,
                              BlockRepository blockRepository,
                              UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReportDto createReport(Long reporterId, CreateReportRequest request) {
        if (reporterId.equals(request.getReportedUserId())) {
            throw new BadRequestException("You cannot report yourself");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporter user not found"));
        User reported = userRepository.findById(request.getReportedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Reported user not found"));

        Report report = new Report(reporter, reported, request.getReasonCategory(), request.getDescription().trim());
        Report saved = reportRepository.save(report);
        return DtoMapper.toReportDto(saved);
    }

    @Transactional
    public BlockDto blockUser(Long blockerId, BlockRequest request) {
        if (blockerId.equals(request.getBlockedUserId())) {
            throw new BadRequestException("You cannot block yourself");
        }

        Optional<Block> existing = blockRepository.findByBlockerIdAndBlockedId(blockerId, request.getBlockedUserId());
        if (existing.isPresent()) {
            return DtoMapper.toBlockDto(existing.get());
        }

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new ResourceNotFoundException("Blocker user not found"));
        User blocked = userRepository.findById(request.getBlockedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        Block block = new Block(blocker, blocked);
        Block saved = blockRepository.save(block);
        return DtoMapper.toBlockDto(saved);
    }

    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        Optional<Block> existing = blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId);
        existing.ifPresent(blockRepository::delete);
    }
}
