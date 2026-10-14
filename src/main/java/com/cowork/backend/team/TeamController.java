package com.cowork.backend.team;

import com.cowork.backend.team.dto.CreateTeamRequest;
import com.cowork.backend.team.dto.InviteRequest;
import com.cowork.backend.team.dto.TeamDto;
import com.cowork.backend.team.dto.TeamMemberDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<TeamDto> create(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.create(request));
    }

    @GetMapping("/mine")
    public List<TeamDto> mine() {
        return teamService.mine();
    }

    @GetMapping("/{id}/members")
    public List<TeamMemberDto> members(@PathVariable Long id) {
        return teamService.members(id);
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<Void> invite(@PathVariable Long id, @Valid @RequestBody InviteRequest request) {
        teamService.invite(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
