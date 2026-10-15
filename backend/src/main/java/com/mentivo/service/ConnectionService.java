package com.mentivo.service;

import com.mentivo.dto.connection.ConnectionDto;
import com.mentivo.dto.connection.CreateConnectionRequest;
import com.mentivo.entity.Connection;
import com.mentivo.entity.User;
import com.mentivo.entity.enums.ConnectionStatus;
import com.mentivo.entity.enums.NotificationType;
import com.mentivo.exception.BadRequestException;
import com.mentivo.exception.ConflictException;
import com.mentivo.exception.ResourceNotFoundException;
import com.mentivo.exception.UnauthorizedException;
import com.mentivo.repository.BlockRepository;
import com.mentivo.repository.ConnectionRepository;
import com.mentivo.repository.UserRepository;
import com.mentivo.util.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final NotificationService notificationService;

    public ConnectionService(ConnectionRepository connectionRepository,
                             UserRepository userRepository,
                             BlockRepository blockRepository,
                             NotificationService notificationService) {
        this.connectionRepository = connectionRepository;
        this.userRepository = userRepository;
        this.blockRepository = blockRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ConnectionDto sendConnectionRequest(Long senderId, CreateConnectionRequest request) {
        if (senderId.equals(request.getReceiverId())) {
            throw new BadRequestException("You cannot connect with yourself");
        }

        if (blockRepository.isBlockedBetween(senderId, request.getReceiverId())) {
            throw new BadRequestException("Unable to send connection request to this user");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender user not found"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient user not found"));

        Optional<Connection> existingOpt = connectionRepository.findConnectionBetween(senderId, request.getReceiverId());
        if (existingOpt.isPresent()) {
            Connection existing = existingOpt.get();
            if (existing.getStatus() == ConnectionStatus.ACCEPTED) {
                throw new ConflictException("You are already connected with this user");
            }
            if (existing.getStatus() == ConnectionStatus.PENDING) {
                throw new ConflictException("A connection request is already pending with this user");
            }
            // If previous was rejected or cancelled, reactivate it as pending
            existing.setSender(sender);
            existing.setReceiver(receiver);
            existing.setMessage(request.getMessage());
            existing.setStatus(ConnectionStatus.PENDING);
            Connection updated = connectionRepository.save(existing);
            notifyReceiver(sender, receiver, updated.getId());
            return DtoMapper.toConnectionDto(updated);
        }

        Connection connection = new Connection(sender, receiver, request.getMessage());
        Connection saved = connectionRepository.save(connection);

        notifyReceiver(sender, receiver, saved.getId());

        return DtoMapper.toConnectionDto(saved);
    }

    private void notifyReceiver(User sender, User receiver, Long connectionId) {
        notificationService.createNotification(
                receiver,
                "New Connection Request",
                sender.getFullName() + " wants to connect with you on Mentivo.",
                NotificationType.CONNECTION_REQUEST,
                connectionId
        );
    }

    @Transactional(readOnly = true)
    public Page<ConnectionDto> getMyConnections(Long userId, Pageable pageable) {
        return connectionRepository.findActiveConnectionsForUser(userId, pageable)
                .map(DtoMapper::toConnectionDto);
    }

    @Transactional(readOnly = true)
    public Page<ConnectionDto> getIncomingRequests(Long userId, Pageable pageable) {
        return connectionRepository.findByReceiverIdAndStatus(userId, ConnectionStatus.PENDING, pageable)
                .map(DtoMapper::toConnectionDto);
    }

    @Transactional
    public ConnectionDto acceptConnection(Long userId, Long connectionId) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found with ID: " + connectionId));

        if (!connection.getReceiver().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to accept this connection request");
        }

        connection.setStatus(ConnectionStatus.ACCEPTED);
        Connection updated = connectionRepository.save(connection);

        notificationService.createNotification(
                connection.getSender(),
                "Connection Accepted",
                connection.getReceiver().getFullName() + " accepted your connection request.",
                NotificationType.CONNECTION_ACCEPTED,
                updated.getId()
        );

        return DtoMapper.toConnectionDto(updated);
    }

    @Transactional
    public ConnectionDto rejectConnection(Long userId, Long connectionId) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found with ID: " + connectionId));

        if (!connection.getReceiver().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to reject this connection request");
        }

        connection.setStatus(ConnectionStatus.REJECTED);
        Connection updated = connectionRepository.save(connection);
        return DtoMapper.toConnectionDto(updated);
    }
}
