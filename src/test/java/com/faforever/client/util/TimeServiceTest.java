package com.faforever.client.util;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.DateInfo;
import com.faforever.client.preferences.LocalizationPrefs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.FormatStyle;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith({MockitoExtension.class})
class TimeServiceTest {

  @Mock
  I18n i18n;
  @Mock
  ChatPrefs chatPrefs;
  @Mock
  LocalizationPrefs localizationPrefs;

  @InjectMocks
  TimeService service;

  private final Locale localeUS = Locale.of("en", "US");
  private final Locale localeFR = Locale.of("fr", "FR");
  private final FormatStyle formatStyleMedium = FormatStyle.MEDIUM;

  @Test
  void asDateAutoUS() {
    var date = generateDate();
    var dateInfo = DateInfo.AUTO;

    when(localizationPrefs.getDateFormat()).thenReturn(dateInfo);
    when(i18n.getUserSpecificLocale()).thenReturn(localeUS);

    var result = service.asDate(date, formatStyleMedium);

    assertEquals("May 9, 2022", result);
  }

  @Test
  void asDateMonthDayYearUS() {
    var date = generateDate();
    var dateInfo = DateInfo.MONTH_DAY_YEAR;

    when(localizationPrefs.getDateFormat()).thenReturn(dateInfo);
    when(i18n.getUserSpecificLocale()).thenReturn(localeUS);

    var result = service.asDate(date, formatStyleMedium);

    assertEquals("May 9, 2022", result);
  }

  @Test
  void asDateDayMonthYearUS() {
    var date = generateDate();
    var dateInfo = DateInfo.DAY_MONTH_YEAR;

    when(localizationPrefs.getDateFormat()).thenReturn(dateInfo);
    when(i18n.getUserSpecificLocale()).thenReturn(localeUS);

    var result = service.asDate(date, formatStyleMedium);

    assertEquals("9 May, 2022", result);
  }

  @Test
  void asDateAutoFR() {
    var date = generateDate();
    var dateInfo = DateInfo.AUTO;

    when(localizationPrefs.getDateFormat()).thenReturn(dateInfo);
    when(i18n.getUserSpecificLocale()).thenReturn(localeFR);

    var result = service.asDate(date, formatStyleMedium);

    assertEquals("9 mai 2022", result);
  }

  @Test
  void asDateMonthDayYearFR() {
    var date = generateDate();
    var dateInfo = DateInfo.MONTH_DAY_YEAR;

    when(localizationPrefs.getDateFormat()).thenReturn(dateInfo);
    when(i18n.getUserSpecificLocale()).thenReturn(localeFR);

    var result = service.asDate(date, formatStyleMedium);

    assertEquals("mai 9, 2022", result);
  }

  @Test
  void asDateDayMonthYearFR() {
    var date = generateDate();
    var dateInfo = DateInfo.DAY_MONTH_YEAR;

    when(localizationPrefs.getDateFormat()).thenReturn(dateInfo);
    when(i18n.getUserSpecificLocale()).thenReturn(localeFR);

    var result = service.asDate(date, formatStyleMedium);

    assertEquals("9 mai, 2022", result);
  }

  @Test
  void asDateAutoFullUS() {
    var date = generateDate();
    var dateInfo = DateInfo.AUTO;

    when(localizationPrefs.getDateFormat()).thenReturn(dateInfo);
    when(i18n.getUserSpecificLocale()).thenReturn(localeUS);

    var result = service.asDate(date, FormatStyle.FULL);

    assertEquals("Monday, May 9, 2022", result);
  }

  @Test
  void asDateDMYFullUS() {
    var date = generateDate();
    var dateInfo = DateInfo.DAY_MONTH_YEAR;

    when(localizationPrefs.getDateFormat()).thenReturn(dateInfo);
    when(i18n.getUserSpecificLocale()).thenReturn(localeUS);

    var result = service.asDate(date, FormatStyle.FULL);

    assertEquals("Monday, 9 May, 2022", result);
  }

  @Test
  void asDateDMYFullFR() {
    var date = generateDate();
    var dateInfo = DateInfo.DAY_MONTH_YEAR;

    when(localizationPrefs.getDateFormat()).thenReturn(dateInfo);
    when(i18n.getUserSpecificLocale()).thenReturn(localeFR);

    var result = service.asDate(date, FormatStyle.FULL);

    assertEquals("lundi, 9 mai, 2022", result);
  }


  private OffsetDateTime generateDate() {
    return OffsetDateTime.of(2022, 5, 9, 9, 9, 9, 9, ZoneOffset.UTC);
  }

  @Test
  public void testShortDurationWithoutPrecision() {
    OffsetDateTime fixedDateTime = OffsetDateTime.of(2024, 6, 12, 10, 20, 30, 40, ZoneOffset.UTC);

    Duration duration1 = Duration.between(fixedDateTime, fixedDateTime.plusSeconds(50));
    when(i18n.get("duration.minute")).thenReturn("< 1min");
    assertEquals("< 1min", service.shortDurationWithoutPrecision(duration1));

    Duration duration2 = Duration.between(fixedDateTime, fixedDateTime.plusMinutes(10));
    when(i18n.get("duration.minutes", duration2.toMinutes())).thenReturn("10min");
    assertEquals("10min", service.shortDurationWithoutPrecision(duration2));

    Duration duration3 = Duration.between(fixedDateTime, fixedDateTime.plusHours(1));
    when(i18n.get("duration.hours", duration3.toHours())).thenReturn("6h");
    assertEquals("6h", service.shortDurationWithoutPrecision(duration3));

    Duration duration4 = Duration.between(fixedDateTime, fixedDateTime.plusDays(1));
    when(i18n.get("duration.days", duration4.toDays())).thenReturn("1d");
    assertEquals("1d", service.shortDurationWithoutPrecision(duration4));
  }
}
