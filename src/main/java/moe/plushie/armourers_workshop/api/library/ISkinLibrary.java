package moe.plushie.armourers_workshop.api.library;

import java.util.Collection;
import moe.plushie.armourers_workshop.api.skin.ISkinType;
import moe.plushie.armourers_workshop.api.skin.serializer.ISkinFileHeader;
import org.apache.commons.lang3.tuple.Pair;

@SuppressWarnings("unused")
public interface ISkinLibrary {

    interface Difference {

        Collection<Entry> addedChanges();

        Collection<Entry> removedChanges();

        Collection<Pair<Entry, Entry>> updatedChanges();

    }

    interface Entry {

        String name();

        String path();

        String skinIdentifier();

        ISkinType skinType();

        ISkinFileHeader skinHeader();

        boolean isDirectory();

        boolean isPrivateDirectory();

    }

}

