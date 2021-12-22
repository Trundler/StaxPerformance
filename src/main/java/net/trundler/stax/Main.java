package net.trundler.stax;

import org.apache.commons.io.output.NullOutputStream;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        Main main = new Main();

        final Path path = Paths.get("mondial.xml");
        try {
            System.out.println("size=" + Files.size(path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Find all implementations and add default one
        final List<String> eventFactories = ServiceLoader
                .load(XMLEventFactory.class)
                .stream()
                .map(c -> c.get().getClass().getName())
                .sorted()
                .collect(Collectors.toList());
        eventFactories.add("com.sun.xml.internal.stream.events.XMLEventFactoryImpl");

        final List<String> inputFactories = ServiceLoader
                .load(XMLInputFactory.class)
                .stream()
                .map(c -> c.get().getClass().getName())
                .sorted()
                .collect(Collectors.toList());
        inputFactories.add("com.sun.xml.internal.stream.XMLInputFactoryImpl");

        final List<String> outputFactories = ServiceLoader
                .load(XMLOutputFactory.class)
                .stream()
                .map(c -> c.get().getClass().getName())
                .sorted()
                .collect(Collectors.toList());
        outputFactories.add("com.sun.xml.internal.stream.XMLOutputFactoryImpl");

        // Iterate over implementations
        for(int j = 0; j< eventFactories.size() ; j++) {

            System.setProperty("javax.xml.stream.XMLEventFactory", eventFactories.get(j));
            System.setProperty("javax.xml.stream.XMLInputFactory", inputFactories.get(j));
            System.setProperty("javax.xml.stream.XMLOutputFactory", outputFactories.get(j));

            System.out.println(eventFactories.get(j));

            XMLInputFactory xif = XMLInputFactory.newInstance();
            XMLEventFactory xef = XMLEventFactory.newInstance();
            XMLOutputFactory xof = XMLOutputFactory.newInstance();

            main.doIt(path, xif, xef, xof);

            System.out.println();
            System.out.println("##############################");
            System.out.println();
        }


    }

    private void doIt(Path path, XMLInputFactory xif, XMLEventFactory xef, XMLOutputFactory xof) {
        try {
            List<Long> eventDurations = new ArrayList<>();
            List<Long> streamDurations = new ArrayList<>();

            for(int i=0; i<100; i++) {
                {
                    long start = System.currentTimeMillis();
                    processAsEvents(path, xef, xif, xof);
                    long duration = (System.currentTimeMillis() - start);
                    eventDurations.add(duration);
                }

                {
                    long start = System.currentTimeMillis();
                    processAsStream(path, xef, xif, xof);
                    long duration = (System.currentTimeMillis() - start);
                    streamDurations.add(duration);
                }
            }

            final IntSummaryStatistics eventStats = eventDurations.stream().collect(Collectors.summarizingInt(Long::intValue));
            printStats("Events", eventStats);

            final IntSummaryStatistics streamStats = streamDurations.stream().collect(Collectors.summarizingInt(Long::intValue));
            printStats("Streams", streamStats);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void printStats(String title, IntSummaryStatistics eventStats) {
        System.out.println(title + " avg=" + eventStats.getAverage() + " min=" + eventStats.getMin() + " max=" + eventStats.getMax());
    }


    public void processAsStream(Path path, XMLEventFactory xef, XMLInputFactory xif, XMLOutputFactory xof) throws XMLStreamException, IOException {

        try (final Reader reader = Files.newBufferedReader(path);
             final OutputStream os = new NullOutputStream()) {

            XMLStreamReader streamReader = xif.createXMLStreamReader(reader);
            XMLStreamWriter streamWriter = xof.createXMLStreamWriter(os);

            while (streamReader.hasNext()) {
                streamReader.next();

                final int eventType = streamReader.getEventType();

                switch(eventType) {
                    case XMLStreamReader.START_DOCUMENT:
                        streamWriter.writeStartDocument();
                        break;
                    case XMLStreamReader.END_DOCUMENT:
                        streamWriter.writeEndDocument();
                        break;
                    case XMLStreamReader.START_ELEMENT:
                        streamWriter.writeStartElement(streamReader.getLocalName());
                        break;
                    case XMLStreamReader.END_ELEMENT:
                        streamWriter.writeEndElement();;
                        break;
                    case XMLStreamReader.CHARACTERS:
                        streamWriter.writeCharacters("a");
                        break;
                }

            }
        }
    }

    public void processAsEvents(Path path, XMLEventFactory xef, XMLInputFactory xif, XMLOutputFactory xof) throws XMLStreamException, IOException {

        String currentElement;
        String currentCars;

        try (final Reader reader = Files.newBufferedReader(path);
             final OutputStream os = new NullOutputStream()) {

            XMLEventReader eventReader = xif.createXMLEventReader(reader);
            XMLEventWriter eventWriter = xof.createXMLEventWriter(os);

            while (eventReader.hasNext()) {
                final XMLEvent xmlEvent = eventReader.nextEvent();

                if (xmlEvent.isStartElement()) {
                    final StartElement startElement = xmlEvent.asStartElement();
                    currentElement = startElement.getName().getLocalPart();

                } else if (xmlEvent.isCharacters()) {
                    final Characters characters = xmlEvent.asCharacters();
                    currentCars = characters.getData();
                }
                eventWriter.add(xmlEvent);
            }
        }
    }
}
