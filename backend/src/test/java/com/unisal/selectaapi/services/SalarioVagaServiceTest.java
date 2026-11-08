package com.unisal.selectaapi.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import com.unisal.selectaapi.company.entity.CompanyProfile;
import com.unisal.selectaapi.company.repository.CompanyProfileRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.geo.EnderecoDto;
import com.unisal.selectaapi.geo.GeoCodingService;
import com.unisal.selectaapi.vagas.VagaRole;
import com.unisal.selectaapi.vagas.dto.*;
import com.unisal.selectaapi.vagas.entity.*;
import com.unisal.selectaapi.vagas.repository.*;
import com.unisal.selectaapi.vagas.services.VagaService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SalarioVagaServiceTest {
    private final VagaRepository vagas = mock(VagaRepository.class);
    private final CompanyProfileRepository companies = mock(CompanyProfileRepository.class);
    private final GeoCodingService geo = mock(GeoCodingService.class);
    private final TipoDeEtapaRepository etapas = mock(TipoDeEtapaRepository.class);
    private final VagaService service = new VagaService(vagas, companies, geo, null, null, null, etapas);

    @Test
    void createsNegotiableSalaryWithoutAmountAndExposesFlagInJson() {
        when(companies.findById(any())).thenReturn(Optional.of(new CompanyProfile()));
        when(geo.retornarEndereco(anyDouble(), anyDouble())).thenReturn(new EnderecoDto("Lorena", "SP"));
        when(etapas.findByCodigoAndCompanyProfileIsNull(anyString())).thenReturn(Optional.of(new TipoDeEtapa()));
        when(etapas.findAllSystemEtapas()).thenReturn(List.of());
        when(vagas.save(any())).thenAnswer(call -> call.getArgument(0));
        var result = service.createVaga(new CreateVagaDto("Analista", null, 1, "Descrição", "Benefícios",
                UUID.randomUUID(), VagaRole.DESENVOLVIMENTO_SOFTWARE, ModalidadeVaga.REMOTO,
                -23.0, -45.0, List.of(), true));
        assertEquals(0, result.salario().signum());
        assertTrue(result.isSalarioACombinar());
        assertTrue(JsonMapper.builder().build().valueToTree(result).get("salarioACombinar").asBoolean());
    }

    @Test
    void changingSalaryAndUnrelatedUpdatesPreserveTheExpectedState() {
        Vaga vaga = persisted(new BigDecimal("5000"));
        var negotiated = service.updateVaga(vaga.getId(), update(null, true));
        assertTrue(negotiated.isSalarioACombinar());
        assertTrue(service.updateVaga(vaga.getId(), update(null, null)).isSalarioACombinar());
        var fixed = service.updateVaga(vaga.getId(), update(new BigDecimal("6500"), false));
        assertFalse(fixed.isSalarioACombinar());
        assertEquals(new BigDecimal("6500"), fixed.salario());
    }

    @Test
    void disablingNegotiableSalaryRequiresAPositiveAmount() {
        Vaga vaga = persisted(BigDecimal.ZERO);
        assertThrows(BusinessException.class, () -> service.updateVaga(vaga.getId(), update(null, false)));
        assertThrows(BusinessException.class, () -> service.updateVaga(vaga.getId(), update(BigDecimal.ZERO, false)));
        assertThrows(BusinessException.class, () -> service.updateVaga(vaga.getId(), update(BigDecimal.valueOf(-1), null)));
        assertEquals(0, vaga.getSalario().signum());
    }

    private Vaga persisted(BigDecimal salary) {
        var vaga = new Vaga();
        vaga.setId(UUID.randomUUID());
        vaga.setEmpresa(new CompanyProfile());
        vaga.setSalario(salary);
        when(vagas.findById(vaga.getId())).thenReturn(Optional.of(vaga));
        when(vagas.save(any())).thenAnswer(call -> call.getArgument(0));
        return vaga;
    }

    private UpdateVaga update(BigDecimal salary, Boolean negotiable) {
        return new UpdateVaga(null, salary, null, null, null, null, null, null, null, null, negotiable);
    }
}
