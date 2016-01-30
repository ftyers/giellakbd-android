/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package so.brendan.hfstospell;

import android.view.textservice.TextInfo;
import android.view.textservice.SuggestionsInfo;
import android.service.textservice.SpellCheckerService;
import android.util.Log;

import java.lang.Override;

import fi.helsinki.hfst.StringWeightPairVector;
import fi.helsinki.hfst.ZHfstOspeller;

/**
 * Service for spell checking, using HFST dictionaries.
 */
public final class HfstSpellCheckerService extends SpellCheckerService {
    private static final String TAG = HfstSpellCheckerService.class.getSimpleName();


    public HfstSpellCheckerService() {
        super();
    }

    static {
        HfstUtils.loadNativeLibrary();
    }

    @Override
    public void onCreate() {
        HfstUtils.init(this);
    }

    @Override
    public Session createSession() {
        return new HfstSpellCheckerSession();
    }

    private class HfstSpellCheckerSession extends Session {
        private String mLanguage;

        @Override
        public void onCreate() {
            mLanguage = getLocale();
        }

        @Override
        public SuggestionsInfo onGetSuggestions(TextInfo textInfo, int suggestionsLimit) {
            // If the speller isn't ready, return an empty list and an OK message.
            ZHfstOspeller speller = HfstUtils.getSpeller(mLanguage);

            if (speller == null) {
                Log.w(TAG, String.format("Speller for language '%s' wasn't ready", mLanguage));
                return new SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY, new String[0]);
            }

            // If the speller IS ready, do the proper thing.
            String word = textInfo.getText();

            // Check if the word is spelled correctly.
            if (speller.spell(word)) {
                return new SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY, new String[0]);
            }

            // If the word isn't correct, query the C++ spell checker for suggestions.
            StringWeightPairVector suggs = speller.suggest(word);
            String[] suggestions = new String[(int) suggs.size()];

            for (int i = 0; i < suggs.size(); i++) {
                suggestions[i] = suggs.get(i).getFirst();
            }

            int attrs = SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO;

            return new SuggestionsInfo(attrs, suggestions);
        }
    }
}