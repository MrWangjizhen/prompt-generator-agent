package com.yuai.promptgenerator.server.controller;

import java.util.Map;

import com.yuai.promptgenerator.core.repository.AppUserRepository;
import com.yuai.promptgenerator.server.security.CurrentUser;
import io.github.agentmemory.AgentMemoryManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/memory")
public class MemoryController {
    private final AgentMemoryManager memoryManager;
    private final AppUserRepository users;

    public MemoryController(AgentMemoryManager memoryManager, AppUserRepository users) {
        this.memoryManager = memoryManager;
        this.users = users;
    }

    @GetMapping
    Object export(Authentication authentication) {
        return memoryManager.exportUserMemory(CurrentUser.require(authentication).id());
    }

    @PostMapping("/consent")
    Map<String, Boolean> consent(@RequestBody ConsentRequest request, Authentication authentication) {
        String userId = CurrentUser.require(authentication).id();
        var user = users.findById(userId).orElseThrow();
        user.setMemoryConsent(request.enabled());
        users.save(user);
        memoryManager.setDisabled(userId, !request.enabled());
        return Map.of("enabled", request.enabled());
    }

    @DeleteMapping
    ResponseEntity<Void> delete(Authentication authentication) {
        String userId = CurrentUser.require(authentication).id();
        memoryManager.deleteUserMemory(userId);
        users.findById(userId).ifPresent(user -> { user.setMemoryConsent(false); users.save(user); });
        return ResponseEntity.noContent().build();
    }

    public record ConsentRequest(boolean enabled) { }
}
