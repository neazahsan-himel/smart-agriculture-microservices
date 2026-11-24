package com.smartagriculture.knowledgeservice.importer;

import com.smartagriculture.knowledgeservice.dto.ImportReport;
import com.smartagriculture.knowledgeservice.dto.PesticideProductDto;
import com.smartagriculture.knowledgeservice.entity.KnowledgeEnums;
import com.smartagriculture.knowledgeservice.entity.KnowledgeSource;
import com.smartagriculture.knowledgeservice.exception.DuplicateResourceException;
import com.smartagriculture.knowledgeservice.repository.KnowledgeSourceRepository;
import com.smartagriculture.knowledgeservice.service.*;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Row mapping, validation and per-row error reporting; services are mocked, validation is real. */
class KnowledgeImportServiceTest {

    private static final String HEADER = "tradeName,activeIngredient,crop,target,doseValue,doseUnit,doseBasis,"
            + "preHarvestIntervalDays,registrationStatus,sourceTitle,sourcePage\n";

    private ValidatorFactory validatorFactory;
    private KnowledgeSourceRepository sourceRepository;
    private PesticideProductService pesticideService;
    private KnowledgeImportService importService;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        sourceRepository = mock(KnowledgeSourceRepository.class);
        pesticideService = mock(PesticideProductService.class);
        importService = new KnowledgeImportService(validatorFactory.getValidator(), sourceRepository,
                mock(KnowledgeSourceService.class), pesticideService, mock(FertilizerRecommendationService.class),
                mock(CropCalendarEntryService.class), mock(PestDiseaseEntryService.class));
        when(sourceRepository.findByTitleIgnoreCaseAndDeletedFalse("Test List"))
                .thenReturn(List.of(KnowledgeSource.builder().id("src-1").title("Test List").build()));
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void validRow_isMappedWithTypesEnumsAndSourceResolvedByTitle() {
        ImportReport report = importService.importCsv(ImportType.PESTICIDES,
                HEADER + "ProductA,ingredient-a,rice,leaf spot,2,g,per litre of water,7,registered,Test List,12\n");

        assertThat(report.getImported()).isEqualTo(1);
        assertThat(report.getFailed()).isZero();
        ArgumentCaptor<PesticideProductDto.Request> captor = ArgumentCaptor.forClass(PesticideProductDto.Request.class);
        verify(pesticideService).createPesticide(captor.capture());
        PesticideProductDto.Request req = captor.getValue();
        assertThat(req.getDoseValue()).isEqualTo(2.0);
        assertThat(req.getPreHarvestIntervalDays()).isEqualTo(7);
        assertThat(req.getRegistrationStatus()).isEqualTo(KnowledgeEnums.RegistrationStatus.REGISTERED);
        assertThat(req.getSourceId()).isEqualTo("src-1");
        assertThat(req.getSourcePage()).isEqualTo("12");
    }

    @Test
    void badRows_areReportedWithTheirRowNumber_andGoodRowsStillImport() {
        String csv = HEADER
                + "ProductA,ingredient-a,RICE,blast,2,g,per litre,7,REGISTERED,Test List,1\n"   // row 2: ok
                + "ProductB,ingredient-b,RICE,blast,two,g,per litre,7,REGISTERED,Test List,1\n" // row 3: bad number
                + ",ingredient-c,RICE,blast,1,g,per litre,7,REGISTERED,Test List,1\n"          // row 4: missing trade name
                + "ProductD,ingredient-d,RICE,blast,1,g,per litre,7,MAYBE,Test List,1\n"       // row 5: bad enum
                + "ProductE,ingredient-e,RICE,blast,1,g,per litre,7,REGISTERED,Unknown Doc,1\n"; // row 6: unknown source

        ImportReport report = importService.importCsv(ImportType.PESTICIDES, csv);

        assertThat(report.getTotalRows()).isEqualTo(5);
        assertThat(report.getImported()).isEqualTo(1);
        assertThat(report.getFailed()).isEqualTo(4);
        assertThat(report.getErrors()).extracting(ImportReport.RowError::getRow).containsExactly(3, 4, 5, 6);
        assertThat(report.getErrors().get(0).getMessage()).contains("doseValue").contains("not a number");
        assertThat(report.getErrors().get(1).getMessage()).contains("tradeName");
        assertThat(report.getErrors().get(2).getMessage()).contains("registrationStatus").contains("REGISTERED");
        assertThat(report.getErrors().get(3).getMessage()).contains("No source titled 'Unknown Doc'");
    }

    @Test
    void rowWithoutAnySource_failsValidation() {
        ImportReport report = importService.importCsv(ImportType.PESTICIDES,
                HEADER + "ProductA,ingredient-a,RICE,blast,2,g,per litre,7,REGISTERED,,\n");

        assertThat(report.getFailed()).isEqualTo(1);
        assertThat(report.getErrors().get(0).getMessage()).contains("sourceId").contains("every fact must cite its source");
        verifyNoInteractions(pesticideService);
    }

    @Test
    void duplicateFromTheService_isReportedNotThrown() {
        when(pesticideService.createPesticide(any())).thenThrow(new DuplicateResourceException("Pesticide use already exists"));

        ImportReport report = importService.importCsv(ImportType.PESTICIDES,
                HEADER + "ProductA,ingredient-a,RICE,blast,2,g,per litre,7,REGISTERED,Test List,1\n");

        assertThat(report.getFailed()).isEqualTo(1);
        assertThat(report.getErrors().get(0).getMessage()).isEqualTo("Pesticide use already exists");
    }

    @Test
    void ambiguousSourceTitle_asksForSourceId() {
        when(sourceRepository.findByTitleIgnoreCaseAndDeletedFalse("Two Editions")).thenReturn(List.of(
                KnowledgeSource.builder().id("a").build(), KnowledgeSource.builder().id("b").build()));

        ImportReport report = importService.importCsv(ImportType.PESTICIDES,
                HEADER + "ProductA,ingredient-a,RICE,blast,2,g,per litre,7,REGISTERED,Two Editions,1\n");

        assertThat(report.getErrors().get(0).getMessage()).contains("use sourceId");
    }

    @Test
    void unknownImportPath_isRejected() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ImportType.fromPath("prices"))
                .hasMessageContaining("Unknown import type 'prices'")
                .hasMessageContaining("pesticides");
    }
}
