package com.cognizant.storeops.programmes.web;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.cognizant.storeops.programmes.domain.Project;
import com.cognizant.storeops.programmes.domain.ProjectMember;
import com.cognizant.storeops.programmes.domain.ProjectRole;
import com.cognizant.storeops.programmes.service.ProgrammeService;
import com.cognizant.storeops.shared.error.ConflictException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgrammeController.class)
class ProgrammeControllerTest {

    private static final Instant AT = Instant.parse("2026-09-20T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProgrammeService programmeService;

    @Test
    void listsProgrammesForAStore() throws Exception {
        when(programmeService.listForStore("store-12"))
                .thenReturn(List.of(new Project("prog-1", "Autumn reset", "store-12", "user-1", AT)));

        mockMvc.perform(get("/api/programmes").param("storeId", "store-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("prog-1"))
                .andExpect(jsonPath("$[0].name").value("Autumn reset"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void createsAProgrammeAndSetsLocation() throws Exception {
        when(programmeService.create("Autumn reset", "store-12", "user-1"))
                .thenReturn(new Project("prog-1", "Autumn reset", "store-12", "user-1", AT));

        mockMvc.perform(post("/api/programmes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Autumn reset\",\"storeId\":\"store-12\","
                                + "\"ownerId\":\"user-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/programmes/prog-1"))
                .andExpect(jsonPath("$.ownerId").value("user-1"));
    }

    @Test
    void rejectsAProgrammeWithNoName() throws Exception {
        mockMvc.perform(post("/api/programmes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"storeId\":\"store-12\",\"ownerId\":\"user-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void addsAMember() throws Exception {
        when(programmeService.addMember(eq("prog-1"), eq("user-7"), any()))
                .thenReturn(new ProjectMember("prog-1", "user-7", ProjectRole.ASSOCIATE));

        mockMvc.perform(post("/api/programmes/prog-1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user-7\",\"role\":\"ASSOCIATE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("user-7"))
                .andExpect(jsonPath("$.role").value("ASSOCIATE"));
    }

    @Test
    void mapsADuplicateMemberOntoA409WithItsCode() throws Exception {
        when(programmeService.addMember(eq("prog-1"), eq("user-7"), any()))
                .thenThrow(new ConflictException("CONFLICT", "User user-7 is already a member"));

        mockMvc.perform(post("/api/programmes/prog-1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user-7\",\"role\":\"ASSOCIATE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("User user-7 is already a member"));
    }
}
