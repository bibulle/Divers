package com.em.oltogg;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.component.VEvent;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * The starting point
 */
public class Main {

    /**
     * Directory to Google store user credentials.
     */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(".");
    //new java.io.File(System.getProperty("user.home"), ".store/calendar_sample");

    private static final String DEFAULT_CONFIG_FILE = "config.properties";

    private static final String KEY_ICS_URL = "icsUrl";
    private static final String KEY_GOOGLE_CALENDAR = "googleCalendar";
    private static final String KEY_SYNC_FUTURE = "syncDaysInFuture";
    private static final String KEY_SYNC_PAST = "syncDaysInPast";

    public static void main(String[] args) {


        // Read the parameters to get config
        Properties mainProperties = getConfig(args, DEFAULT_CONFIG_FILE);


        // Initiate Google communication
        com.google.api.services.calendar.Calendar googleClient = getGoogleClient();
        if (googleClient == null) {
            System.err.println("Something go wrong : googleClient == null");
            System.exit(-1);
        }

        // Get the google calendar
        String calendarId = getGoogleCalendarId(mainProperties, googleClient);

        List<Event> googleEvents = getGoogleEvents(mainProperties, googleClient, calendarId);

        // try to read the ics file
        Calendar icsCalendar = getICSCalendar(mainProperties);

        if (icsCalendar != null) {
            List<Event> icsEvents = getIcsEvents(mainProperties, icsCalendar);

            //System.out.println(icsEvents.get(1));

        }

    }

    private static List<Event> getIcsEvents(Properties mainProperties, Calendar icsCalendar) {
        ComponentList cl = icsCalendar.getComponents();

//            System.out.println(cl.get(2));
//            System.out.println(cl.get(3));
//            System.out.println(cl.get(4));
//            System.out.println(cl.get(cl.size()-1));


        // Calculate the max and min
        int syncDaysFuture = Integer.parseInt(mainProperties.getProperty(KEY_SYNC_FUTURE, "600"));
        int syncDaysPast = Integer.parseInt(mainProperties.getProperty(KEY_SYNC_PAST, "90"));

        ZonedDateTime instantFuture = Instant.now()
                                             .plus(syncDaysFuture, DAYS)
                                             .atZone(ZoneId.systemDefault());
        ZonedDateTime instantPast = Instant.now()
                                           .minus(syncDaysPast, DAYS)
                                           .atZone(ZoneId.systemDefault());

        DateTimeFormatter eventDateFormatter1 = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssVV");
        DateTimeFormatter eventDateFormatter2 = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        DateTimeFormatter eventDateFormatter3 = DateTimeFormatter.ofPattern("yyyyMMdd");
        Stream<VEvent> vEvents = cl.stream()
                                   .filter(o -> o instanceof VEvent)
                                   .map(VEvent.class::cast);


        Stream<Event> events = vEvents.map(vEvent -> {

            return getEventFromVEvent(vEvent);

        });

        // filter on start date
        events = events.filter(event -> {
            ZonedDateTime dateStart;
            if (event.getStart()
                     .getDate() != null) {
                dateStart = Instant.ofEpochMilli(event.getStart()
                                                      .getDate()
                                                      .getValue())
                                   .atZone(ZoneId.systemDefault());
            } else {
                dateStart = Instant.ofEpochMilli(event.getStart()
                                                      .getDateTime()
                                                      .getValue())
                                   .atZone(ZoneId.systemDefault());
            }

            return instantFuture.isAfter(dateStart) && instantPast.isBefore(dateStart);
        });


        List<Event> icsEvents = events.collect(Collectors.toList());
        System.out.println("ICS event found : " + icsEvents.size());


        return icsEvents;
    }

    /**
     * Create a google event from the ics event
     *
     * @param vEvent
     * @return
     */
    private static Event getEventFromVEvent(VEvent vEvent) {
        Event event = new Event();

        event.setSummary(vEvent.getSummary()
                               .getValue());

        Date startDate = vEvent.getStartDate()
                               .getDate();

        DateTime start = new DateTime(startDate, (vEvent.getStartDate()
                                                        .getTimeZone() == null ? TimeZone.getTimeZone("UTC") : vEvent.getStartDate()
                                                                                                                     .getTimeZone()));
        event.setStart(new EventDateTime().setDateTime(start));
        //System.out.println(event.getStart());

        Date endDate = vEvent.getEndDate()
                             .getDate();
        DateTime end = new DateTime(endDate, (vEvent.getEndDate()
                                                    .getTimeZone() == null ? TimeZone.getTimeZone("UTC") : vEvent.getEndDate()
                                                                                                                 .getTimeZone()));
        event.setEnd(new EventDateTime().setDateTime(end));

        if (vEvent.getDescription() != null) {
            event.setDescription(vEvent.getDescription()
                                       .getValue());
        }

        System.out.println(vEvent.getSequence());
        System.out.println(vEvent);
        System.out.println(vEvent.calculateRecurrenceSet(new Period( new net.fortuna.ical4j.model.DateTime(new Date(1970,0,1)), new net.fortuna.ical4j.model.DateTime(new Date(2070,0,1)))));

        return event;
    }

    private static ZonedDateTime getDateTimeFromVEvent(DateTimeFormatter eventDateFormatter1, DateTimeFormatter eventDateFormatter2, DateTimeFormatter eventDateFormatter3, String dateString) {
        ZonedDateTime dateStart = null;
        try {
            dateStart = ZonedDateTime.parse(dateString, eventDateFormatter1);
        } catch (DateTimeParseException e1) {
            try {
                dateStart = LocalDate.parse(dateString, eventDateFormatter2)
                                     .atStartOfDay(ZoneId.systemDefault());
            } catch (DateTimeParseException e2) {
                //System.err.println(e2.getMessage());
                try {
                    dateStart = LocalDate.parse(dateString, eventDateFormatter3)
                                         .atStartOfDay(ZoneId.systemDefault());
                } catch (DateTimeParseException e3) {
                    System.err.println("Error in ICS format - StartDate : " + e3.getMessage());
                }
            }
        }
        return dateStart;
    }

    /**
     * Get google events between max and min
     *
     * @param mainProperties the main properties
     * @param googleClient   the google calendar client
     * @param calendarId     the calendarId
     * @return the events
     */
    private static List<Event> getGoogleEvents(Properties mainProperties, com.google.api.services.calendar.Calendar googleClient, String calendarId) {

        try {
            // Calculate the max and min
            int syncDaysFuture = Integer.parseInt(mainProperties.getProperty(KEY_SYNC_FUTURE, "600"));
            int syncDaysPast = Integer.parseInt(mainProperties.getProperty(KEY_SYNC_PAST, "90"));

            ZonedDateTime instantFuture = Instant.now()
                                                 .plus(syncDaysFuture, DAYS)
                                                 .atZone(ZoneId.systemDefault());
            ZonedDateTime instantPast = Instant.now()
                                               .minus(syncDaysPast, DAYS)
                                               .atZone(ZoneId.systemDefault());

            Events feed = googleClient.events()
                                      .list(calendarId)
                                      .setMaxResults(2000)
                                      .setSingleEvents(true)
                                      .setOrderBy("startTime")
                                      .setTimeMin(new com.google.api.client.util.DateTime(instantPast.format(DateTimeFormatter.ISO_INSTANT)))
                                      .setTimeMax(new com.google.api.client.util.DateTime(instantFuture.format(DateTimeFormatter.ISO_INSTANT)))
                                      .execute();
            List<Event> items = feed.getItems();

            // filter
            Stream<Event> events = items.stream()
                                        .filter(event -> {

                                            ZonedDateTime dateStart;
                                            if (event.getStart()
                                                     .getDate() != null) {
                                                dateStart = Instant.ofEpochMilli(event.getStart()
                                                                                      .getDate()
                                                                                      .getValue())
                                                                   .atZone(ZoneId.systemDefault());
                                            } else {
                                                dateStart = Instant.ofEpochMilli(event.getStart()
                                                                                      .getDateTime()
                                                                                      .getValue())
                                                                   .atZone(ZoneId.systemDefault());
                                            }

                                            return instantFuture.isAfter(dateStart) && instantPast.isBefore(dateStart);

                                        });

            List<Event> googleEvents = events.collect(Collectors.toList());

            System.out.println("Google event found : " + googleEvents.size());

            //System.out.println(googleEvents.get(0));

            return googleEvents;

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return null;
    }

    /**
     * Get the google calendar Id
     *
     * @param mainProperties the properties file content
     * @param googleClient   the google client object
     * @return the calendar Id
     */
    private static String getGoogleCalendarId(Properties mainProperties, com.google.api.services.calendar.Calendar googleClient) {
        String calendarId = null;
        if ((mainProperties.getProperty(KEY_GOOGLE_CALENDAR) == null) || (mainProperties.getProperty(KEY_GOOGLE_CALENDAR)
                                                                                        .isEmpty())) {
            System.err.println("Wrong parameter in the configuration file - '" + KEY_GOOGLE_CALENDAR + "' : '" + mainProperties.getProperty(KEY_GOOGLE_CALENDAR) + "'");
            System.exit(-1);
        }
        try {
            // getting calendar list
            CalendarList feed = googleClient.calendarList()
                                            .list()
                                            .execute();

            // find the targeted calendar
            if (feed.getItems() != null) {
                for (CalendarListEntry entry : feed.getItems()) {
                    if (entry.getSummary()
                             .equals(mainProperties.getProperty(KEY_GOOGLE_CALENDAR))) {
                        calendarId = entry.getId();
                        break;
                    }
                }
            }

            if (calendarId == null) {
                System.err.println("Calendar not found : '" + mainProperties.getProperty(KEY_GOOGLE_CALENDAR) + "'");
                System.exit(-1);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return calendarId;
    }

    /**
     * Read the ICS file and get the ICSCalendar
     *
     * @param mainProperties the properties file content
     * @return an ICS calendar
     */
    private static Calendar getICSCalendar(Properties mainProperties) {
        Calendar calendar = null;

        try {

            URL icsUrl = new URL(mainProperties.getProperty(KEY_ICS_URL));
            InputStreamReader isr = new InputStreamReader(icsUrl.openStream());
            CalendarBuilder builder = new CalendarBuilder();

            calendar = builder.build(isr);

        } catch (MalformedURLException e) {
            System.err.println("Wrong parameter in the configuration file - '" + KEY_ICS_URL + "' : " + e.getMessage());
            System.exit(-1);

        } catch (ParserException e) {

            System.err.println("Wrong Ics file format : " + e.getMessage());
            System.exit(-1);
        } catch (IOException e) {

            System.err.println("Cannot retrieve ics file : " + e.getMessage());
            System.exit(-1);
        }
        return calendar;
    }

    /**
     * Authenticate and get the google calendar client
     *
     * @return a google calendar client
     */
    private static com.google.api.services.calendar.Calendar getGoogleClient() {
        // initialize the data store factory
        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

        try {

            // initialize the transport
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            // initialize the data store factory
            FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);


            // load googleClient secrets
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                    JSON_FACTORY,
                    new InputStreamReader(new FileInputStream("client_secret.json")));
            //new InputStreamReader(Main.class.getResourceAsStream("/client_secret.json")));
            if (clientSecrets.getDetails()
                             .getClientId()
                             .startsWith("Enter")
                    || clientSecrets.getDetails()
                                    .getClientSecret()
                                    .startsWith("Enter ")) {
                System.out.println();
                System.out.println(
                        "Enter Client ID and Secret from https://console.developers.google.com/ "
                                + "into client_secrets.json");
                System.exit(1);
            }

            // set up authorization code flow
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JacksonFactory.getDefaultInstance(), clientSecrets,
                    Collections.singleton(CalendarScopes.CALENDAR)).setDataStoreFactory(dataStoreFactory)
                                                                   .build();

            // authorize
            Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

            return new com.google.api.services.calendar.Calendar.Builder(
                    httpTransport, JSON_FACTORY, credential).setApplicationName("OlToGg")
                                                            .build();

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    /**
     * Get the configuration from command line args and config file
     *
     * @param args       the main args
     * @param configFile the config file
     * @return the config the main properties
     */
    private static Properties getConfig(String[] args, String configFile) {
        Properties properties = new Properties();

        // Get current jar name and commandline
        String currentJarName = new File(Main.class.getProtectionDomain()
                                                   .getCodeSource()
                                                   .getLocation()
                                                   .getPath()).getName();
        String cmdLineSyntax = "java " + Main.class.getName();
        if (currentJarName.endsWith(".jar")) {
            cmdLineSyntax = "java -jar " + currentJarName;
        }


        // Check parameters
        Options options = new Options();

        Option cOption = new Option("c", "configFile", true, "the properties file needed configure the tool (default : config.properties)");
        Option hOption = new Option("h", "help", false, "Display this help");

        options.addOption(cOption);
        options.addOption(hOption);

        CommandLineParser parser = new DefaultParser();


        try {
            CommandLine cmd = parser.parse(options, args);

            // Help option
            if (cmd.hasOption('h')) {
                new HelpFormatter().printHelp(cmdLineSyntax, options);
                System.exit(0);
            }

            // ConfigFile option
            if (cmd.hasOption('c')) {
                configFile = cmd.getOptionValue('c');
            }


        } catch (ParseException e) {
            System.err.println("Wrong parameters : " + e.getMessage());
            new HelpFormatter().printHelp(cmdLineSyntax, options);

            System.exit(-1);
        }


        // try to read the properties file
        try {
            InputStream input = new FileInputStream(configFile);
            properties.load(input);
        } catch (IOException e) {
            System.err.println("Cannot read configuration : " + e.getMessage());
            System.exit(-1);
        }

        return properties;
    }
}
