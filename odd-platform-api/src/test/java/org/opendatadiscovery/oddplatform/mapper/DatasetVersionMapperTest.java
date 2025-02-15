package org.opendatadiscovery.oddplatform.mapper;

import java.time.ZoneOffset;
import java.util.List;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendatadiscovery.oddplatform.api.contract.model.DataSetField;
import org.opendatadiscovery.oddplatform.api.contract.model.DataSetFieldStat;
import org.opendatadiscovery.oddplatform.api.contract.model.DataSetFieldType;
import org.opendatadiscovery.oddplatform.api.contract.model.DataSetStructure;
import org.opendatadiscovery.oddplatform.api.contract.model.DataSetVersion;
import org.opendatadiscovery.oddplatform.dto.DatasetFieldDto;
import org.opendatadiscovery.oddplatform.dto.DatasetStructureDto;
import org.opendatadiscovery.oddplatform.dto.TagDto;
import org.opendatadiscovery.oddplatform.dto.ingestion.DataEntityIngestionDto;
import org.opendatadiscovery.oddplatform.dto.ingestion.EnrichedDataEntityIngestionDto;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DatasetFieldPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DatasetVersionPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.TagPojo;
import org.opendatadiscovery.oddplatform.utils.JSONTestUtils;

import static org.jooq.JSONB.jsonb;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class DatasetVersionMapperTest {

    @InjectMocks
    DatasetVersionMapper datasetVersionMapper =
        new DatasetVersionMapperImpl(
            new DatasetFieldApiMapperImpl(new TagMapperImpl(),
                new MetadataFieldValueMapperImpl(new MetadataFieldMapperImpl()), new TermMapperImpl(
                new NamespaceMapperImpl(),
                new DateTimeMapperImpl(),
                new OwnershipMapperImpl(
                    new OwnerMapperImpl(),
                    new TitleMapperImpl()
                )
            )),
            new DateTimeMapperImpl());
    private static final EasyRandom EASY_RANDOM;

    static {
        final EasyRandomParameters easyRandomParameters = new EasyRandomParameters();
        EASY_RANDOM = new EasyRandom(easyRandomParameters);
    }

    @Test
    @DisplayName("mapping dataset versions")
    void testDatasetVersion() {
        final DatasetVersionPojo datasetVersionPojo = EASY_RANDOM.nextObject(DatasetVersionPojo.class);

        final DataSetVersion dataSetVersion = datasetVersionMapper.mapPojo(datasetVersionPojo);

        assertEquals(datasetVersionPojo.getVersion().intValue(), dataSetVersion.getVersion());
    }

    @Test
    @DisplayName("mapping dataset versions structure")
    void testDatasetVersionMapDatasetStructure() {
        final DatasetVersionPojo datasetVersionPojo = EASY_RANDOM.nextObject(DatasetVersionPojo.class);
        final DatasetFieldPojo datasetFieldPojo = EASY_RANDOM.nextObject(DatasetFieldPojo.class);
        final DataSetFieldStat dataSetFieldStat = EASY_RANDOM.nextObject(DataSetFieldStat.class);
        final DataSetFieldType dataSetFieldType = EASY_RANDOM.nextObject(DataSetFieldType.class);
        final TagPojo tagPojo = EASY_RANDOM.nextObject(TagPojo.class);

        final DatasetFieldDto datasetFieldDto = new DatasetFieldDto();
        datasetFieldDto.setDatasetFieldPojo(datasetFieldPojo);
        datasetFieldDto.setParentFieldId(1L);
        datasetFieldDto.setEnumValueCount(2);
        datasetFieldDto.setTags(List.of(new TagDto(tagPojo, 1L,  false)));
        datasetFieldDto.getDatasetFieldPojo().setType(jsonb(JSONTestUtils.createJson(dataSetFieldType)));
        datasetFieldDto.getDatasetFieldPojo().setStats(jsonb(JSONTestUtils.createJson(dataSetFieldStat)));

        final DatasetStructureDto expectedDatasetStructureDto =
            new DatasetStructureDto(datasetVersionPojo, List.of(datasetFieldDto));

        final DataSetStructure actualDataSetStructure =
            datasetVersionMapper.mapDatasetStructure(expectedDatasetStructureDto);

        assertDatasetVersions(expectedDatasetStructureDto.getDatasetVersion(),
            actualDataSetStructure.getDataSetVersion());
        final List<DataSetField> actualFieldList = actualDataSetStructure.getFieldList();
        actualFieldList.forEach(actual -> {
            assertNotNull(actual.getId());
            assertNotNull(actual.getEnumValueCount());
            assertNotNull(actual.getExternalDescription());
            assertNotNull(actual.getInternalDescription());
            assertNotNull(actual.getName());
            assertNotNull(actual.getOddrn());
        });
    }

    @Test
    void testMapNewDatasetVersion() {
        final DataEntityIngestionDto dataEntityIngestionDto = new DataEntityIngestionDto();
        dataEntityIngestionDto.setDataSet(
            new DataEntityIngestionDto.DataSetIngestionDto(
                "parentOddrn", null, "structHash", 1L));
        final long expectedVersion = 1L;
        final EnrichedDataEntityIngestionDto dto =
            new EnrichedDataEntityIngestionDto(expectedVersion, null, dataEntityIngestionDto);
        final DatasetVersionPojo actualDatasetVersionPojo =
            datasetVersionMapper.mapDatasetVersion(dto.getOddrn(), dto.getDataSet().structureHash(), expectedVersion);

        assertEquals(expectedVersion, actualDatasetVersionPojo.getVersion());
        assertEquals(dto.getOddrn(), actualDatasetVersionPojo.getDatasetOddrn());
        assertEquals(dto.getDataSet().structureHash(),
            actualDatasetVersionPojo.getVersionHash());
    }

    private void assertDatasetVersions(final DatasetVersionPojo expectedDatasetVersion,
                                       final DataSetVersion actualDataSetVersion) {
        assertEquals(expectedDatasetVersion.getVersion().intValue(), actualDataSetVersion.getVersion());
        assertEquals(expectedDatasetVersion.getId(), actualDataSetVersion.getId());
        assertEquals(expectedDatasetVersion.getCreatedAt().atOffset(ZoneOffset.UTC),
            actualDataSetVersion.getCreatedAt());
    }
}