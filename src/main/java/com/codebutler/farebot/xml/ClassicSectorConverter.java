package com.codebutler.farebot.xml;

import com.codebutler.farebot.card.classic.ClassicSector;
import com.codebutler.farebot.card.classic.InvalidClassicSector;
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector;

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

public class ClassicSectorConverter implements Converter<ClassicSector> {
    @Override public ClassicSector read(InputNode node) throws Exception {
        int sectorIndex = Integer.parseInt(node.getAttribute("index").getValue());

        if (node.getAttribute("unauthorized") != null && node.getAttribute("unauthorized").getValue().equals("true")) {
            return new UnauthorizedClassicSector(sectorIndex);
        }

        if (node.getAttribute("invalid") != null && node.getAttribute("invalid").getValue().equals("true")) {
            return new InvalidClassicSector(sectorIndex, node.getAttribute("error").getValue());
        }

        throw new SkippableRegistryStrategy.SkipException();
    }

    @Override public void write(OutputNode node, ClassicSector value) throws Exception {
        throw new SkippableRegistryStrategy.SkipException();
    }
}
