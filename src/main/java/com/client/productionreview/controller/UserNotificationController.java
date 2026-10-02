package com.client.productionreview.controller;

import com.client.productionreview.dtos.notification.NotificationResponseDTO;
import com.client.productionreview.dtos.notification.UnreadCountDTO;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.NotificationService;
import com.client.productionreview.utils.PaginationUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Notificações do usuário logado (sino do site). O SSE antigo continua em /notification/sse. */
@RestController
@RequestMapping("/api/v1/notifications")
@SecurityRequirement(name = "jwt_auth")
public class UserNotificationController {

    private final NotificationService notificationService;

    public UserNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Page<NotificationResponseDTO> list(@RequestParam(value = "page", required = false) Integer page,
                                              @RequestParam(value = "size", required = false) Integer size,
                                              @RequestParam(value = "unreadOnly", required = false, defaultValue = "false") boolean unreadOnly,
                                              @AuthenticationPrincipal User user) {
        return notificationService.list(user.getId(), unreadOnly, PaginationUtils.createPageable(page, size, null, null));
    }

    @GetMapping("/unread-count")
    public UnreadCountDTO unreadCount(@AuthenticationPrincipal User user) {
        return new UnreadCountDTO(notificationService.unreadCount(user.getId()));
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable("id") Long id, @AuthenticationPrincipal User user) {
        notificationService.markRead(id, user.getId());
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal User user) {
        notificationService.markAllRead(user.getId());
    }
}
