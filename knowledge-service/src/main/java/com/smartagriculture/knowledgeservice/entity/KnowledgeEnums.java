package com.smartagriculture.knowledgeservice.entity;

/** Enums shared by the knowledge tables (stored as STRING). */
public final class KnowledgeEnums {

    private KnowledgeEnums() {
    }

    public enum RecordStatus {
        ACTIVE, INACTIVE
    }

    // Bangladesh rice seasons first; RABI/KHARIF cover other crops later
    public enum CropSeason {
        AUS, AMAN, BORO, RABI, KHARIF, YEAR_ROUND
    }

    public enum RegistrationStatus {
        REGISTERED, DEREGISTERED, BANNED, UNKNOWN
    }

    public enum CalendarActivity {
        SEEDBED, SOWING, TRANSPLANTING, FERTILIZING, WEEDING, IRRIGATION, HARVEST, OTHER
    }

    public enum PestDiseaseKind {
        PEST, DISEASE, DEFICIENCY, OTHER
    }
}
