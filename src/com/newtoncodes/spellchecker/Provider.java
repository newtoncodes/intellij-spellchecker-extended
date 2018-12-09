package com.newtoncodes.spellchecker;

import com.intellij.spellchecker.BundledDictionaryProvider;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;


public class Provider implements BundledDictionaryProvider {
    // private static final Logger LOG = Logger.getInstance("#com.newtoncodes.spellchecker.Provider");

    @Override
    public String[] getBundledDictionaries() {
        return new String[] {};
    }

    @SuppressWarnings("RedundantThrows")
    public static List<String> getDictionaries() throws IOException {
        List<String> filenames = new ArrayList<>();

        filenames.add("/hunspell/Bulgarian.dic");

//        for (String name : getResources(".*")) filenames.add("R1 " + name);

        return filenames;
    }


    public static Collection<String> getResources(final String pattern) {
        final ArrayList<String> result = new ArrayList<>();
        final String classPath = System.getProperty("java.class.path", ".");
        final String[] classPathElements = classPath.split(System.getProperty("path.separator"));

        for(final String element : classPathElements){
            result.addAll(getResources(element, Pattern.compile(pattern)));
        }

        return result;
    }

    private static Collection<String> getResources(final String element, final Pattern pattern) {
        final ArrayList<String> result = new ArrayList<>();
        final File file = new File(element);

        if(file.isDirectory()){
            result.addAll(getResourcesFromDirectory(file, pattern));
        }

        return result;
    }

    private static Collection<String> getResourcesFromDirectory(final File directory, final Pattern pattern) {
        final ArrayList<String> result = new ArrayList<>();
        final File[] files = directory.listFiles();
        if (files == null) return result;

        for (final File file : files) {
            if(file.isDirectory()){
                result.addAll(getResourcesFromDirectory(file, pattern));
            } else{
                try{
                    final String fileName = file.getCanonicalPath();
                    final boolean accept = pattern.matcher(fileName).matches();
                    if(accept){
                        result.add(fileName);
                    }
                } catch(final IOException e){
                    throw new Error(e);
                }
            }
        }

        return result;
    }
}
