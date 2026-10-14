package com.cowork.backend.team;

import com.cowork.backend.team.dto.TeamDto;
import com.cowork.backend.team.dto.TeamInvitationDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final TeamService teamService;

    public InvitationController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/mine")
    public List<TeamInvitationDto> mine() {
        return teamService.myInvitations();
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<TeamDto> accept(@PathVariable String token) {
        return ResponseEntity.ok(teamService.accept(token));
    }

    @PostMapping("/{token}/decline")
    public ResponseEntity<Void> decline(@PathVariable String token) {
        teamService.decline(token);
        return ResponseEntity.noContent().build();
    }
}
