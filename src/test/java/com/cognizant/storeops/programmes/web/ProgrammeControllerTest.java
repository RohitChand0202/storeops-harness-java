package com.cognizant.storeops.programmes.web;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.cognizant.storeops.programmes.domain.Project;
import com.cognizant.storeops.programmes.domain.ProjectMember;
import com.cognizant.storeops.programmes.domain.ProjectRole;
import com.cognizant.storeops.programmes.domain.ProjectStatus;
import com.cognizant.storeops.programmes.service.ProgrammeService;
import com.cognizant.storeops.shared.error.ConflictException;
import com.cognizant.storeops.shared.error.ForbiddenException;
import com.cognizant.storeops.shared.domain.StoreOpsUser;
import com.cognizant.storeops.staff.service.StaffService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgrammeController.class)
class ProgrammeControllerTest {

    private static final Instant AT = Instant.parse("2026-09-20T08:00:00Z");
    private static final StoreOpsUser CALLER = new StoreOpsUser(
            "user-1", "store-12", "region-1", "STORE_MANAGER");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProgrammeService programmeService;

    @MockBean
    private StaffService staffService;

    @BeforeEach
    void authenticate() {
        when(staffService.resolveToken("test-token")).thenReturn(CALLER);
    }

    @Test
    void listsProgrammesForAStore() throws Exception {
        when(programmeService.listForStore(CALLER, "store-12"))
                .thenReturn(List.of(new Project("prog-1", "Autumn reset", "store-12", "user-1", AT)));

        mockMvc.perform(get("/api/programmes").header("Authorization", "Bearer test-token")
                        .param("storeId", "store-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("prog-1"))
                .andExpect(jsonPath("$[0].name").value("Autumn reset"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void createsAProgrammeAndSetsLocation() throws Exception {
        when(programmeService.create(CALLER, "Autumn reset", "store-12", "user-1"))
                .thenReturn(new Project("prog-1", "Autumn reset", "store-12", "user-1", AT));

        mockMvc.perform(post("/api/programmes")
                        .header("Authorization", "Bearer test-token")
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
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"storeId\":\"store-12\",\"ownerId\":\"user-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void addsAMember() throws Exception {
        when(programmeService.addMember(eq(CALLER), eq("prog-1"), eq("user-7"), any()))
                .thenReturn(new ProjectMember("prog-1", "user-7", ProjectRole.ASSOCIATE));

        mockMvc.perform(post("/api/programmes/prog-1/members")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user-7\",\"role\":\"ASSOCIATE\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/programmes/prog-1/members/user-7"))
                .andExpect(jsonPath("$.userId").value("user-7"))
                .andExpect(jsonPath("$.role").value("ASSOCIATE"));
    }

    @Test
    void mapsADuplicateMemberOntoA409WithItsCode() throws Exception {
        when(programmeService.addMember(eq(CALLER), eq("prog-1"), eq("user-7"), any()))
                .thenThrow(new ConflictException("CONFLICT", "User user-7 is already a member"));

        mockMvc.perform(post("/api/programmes/prog-1/members")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user-7\",\"role\":\"ASSOCIATE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("User user-7 is already a member"));
    }

    @Test
    void requiresABearerToken() throws Exception {
        mockMvc.perform(get("/api/programmes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void mapsMalformedJsonAndEnumValuesTo400() throws Exception {
        mockMvc.perform(post("/api/programmes")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/programmes/prog-1/members")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user-7\",\"role\":\"OWNER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void closesProgrammeWithAStatusPatch() throws Exception {
        Project closed = new Project("prog-1", "Autumn reset", "store-12", "user-1", AT);
        closed.setStatus(ProjectStatus.CLOSED);
        when(programmeService.close(CALLER, "prog-1", ProjectStatus.CLOSED)).thenReturn(closed);

        mockMvc.perform(patch("/api/programmes/prog-1")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void refusesClosureWithoutStatusOrPermission() throws Exception {
        mockMvc.perform(patch("/api/programmes/prog-1")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        when(programmeService.close(CALLER, "prog-1", ProjectStatus.CLOSED))
                .thenThrow(new ForbiddenException("FORBIDDEN", "Outside store"));
        mockMvc.perform(patch("/api/programmes/prog-1")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
