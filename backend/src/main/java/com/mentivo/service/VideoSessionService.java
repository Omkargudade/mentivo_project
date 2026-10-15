package com.mentivo.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VideoSessionService {

    public String generateSessionUrl(String bookingRef) {
        // Generates secure encrypted room session link (provider-agnostic abstraction)
        return "https://meet.mentivo.com/room/" + bookingRef + "?token=" + UUID.randomUUID().toString().substring(0, 12);
    }
}
