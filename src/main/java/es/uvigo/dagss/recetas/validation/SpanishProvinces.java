package es.uvigo.dagss.recetas.validation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The 52 Spanish provinces (INE codes 01-52, which are also the first two
 * digits of every postal code), with their usual alternative spellings
 * (Spanish / co-official languages). Pure Java, no framework dependencies.
 *
 * <p>Matching ignores case, accents, spaces and punctuation, so
 * "A Coruña", "a coruna", "La Coruña" and "Coruña, A" are all the same province.
 */
public final class SpanishProvinces {

    public record Province(String code, String name) {
    }

    /** {code, canonical name, aliases...} */
    private static final String[][] DATA = {
        {"01", "Álava", "Araba", "Araba/Álava", "Álava/Araba"},
        {"02", "Albacete"},
        {"03", "Alicante", "Alacant", "Alicante/Alacant", "Alacant/Alicante"},
        {"04", "Almería"},
        {"05", "Ávila"},
        {"06", "Badajoz"},
        {"07", "Illes Balears", "Baleares", "Islas Baleares", "Balears", "Balears, Illes"},
        {"08", "Barcelona"},
        {"09", "Burgos"},
        {"10", "Cáceres"},
        {"11", "Cádiz"},
        {"12", "Castellón", "Castelló", "Castellón/Castelló", "Castelló/Castellón"},
        {"13", "Ciudad Real"},
        {"14", "Córdoba"},
        {"15", "A Coruña", "La Coruña", "Coruña", "Coruña, A"},
        {"16", "Cuenca"},
        {"17", "Girona", "Gerona"},
        {"18", "Granada"},
        {"19", "Guadalajara"},
        {"20", "Gipuzkoa", "Guipúzcoa"},
        {"21", "Huelva"},
        {"22", "Huesca"},
        {"23", "Jaén"},
        {"24", "León"},
        {"25", "Lleida", "Lérida"},
        {"26", "La Rioja", "Rioja"},
        {"27", "Lugo"},
        {"28", "Madrid"},
        {"29", "Málaga"},
        {"30", "Murcia"},
        {"31", "Navarra", "Nafarroa", "Navarra/Nafarroa"},
        {"32", "Ourense", "Orense"},
        {"33", "Asturias"},
        {"34", "Palencia"},
        {"35", "Las Palmas", "Palmas, Las"},
        {"36", "Pontevedra"},
        {"37", "Salamanca"},
        {"38", "Santa Cruz de Tenerife"},
        {"39", "Cantabria"},
        {"40", "Segovia"},
        {"41", "Sevilla"},
        {"42", "Soria"},
        {"43", "Tarragona"},
        {"44", "Teruel"},
        {"45", "Toledo"},
        {"46", "Valencia", "València", "Valencia/València", "València/Valencia"},
        {"47", "Valladolid"},
        {"48", "Bizkaia", "Vizcaya"},
        {"49", "Zamora"},
        {"50", "Zaragoza"},
        {"51", "Ceuta"},
        {"52", "Melilla"},
    };

    private static final List<Province> ALL;
    private static final Map<String, Province> BY_CODE = new LinkedHashMap<>();
    private static final Map<String, Province> BY_NAME = new HashMap<>();

    static {
        List<Province> provinces = new ArrayList<>();
        for (String[] row : DATA) {
            Province province = new Province(row[0], row[1]);
            provinces.add(province);
            BY_CODE.put(province.code(), province);
            for (int i = 1; i < row.length; i++) {
                BY_NAME.put(normalize(row[i]), province);
            }
        }
        ALL = Collections.unmodifiableList(provinces);
    }

    private SpanishProvinces() {
    }

    public static List<Province> all() {
        return ALL;
    }

    public static Optional<Province> findByCode(String code) {
        return Optional.ofNullable(code == null ? null : BY_CODE.get(code));
    }

    public static Optional<Province> find(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_NAME.get(normalize(name)));
    }

    public static String canonicalNameOrSame(String name) {
        return find(name).map(Province::name).orElse(name);
    }

    private static String normalize(String text) {
        String withoutAccents = Normalizer.normalize(text.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
