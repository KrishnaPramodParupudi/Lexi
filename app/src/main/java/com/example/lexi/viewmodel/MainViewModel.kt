package com.example.lexi.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lexi.llmengine.GemmaModelHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainViewModel : ViewModel() {
    var isTeacherMode by mutableStateOf(false)
    var llmResult by mutableStateOf("Your analysis will appear here...")
    var isProcessing by mutableStateOf(false)
    var isModelReady by mutableStateOf(false)
    var selectedImageUri by mutableStateOf<Uri?>(null)
    var selectedImageBitmap by mutableStateOf<Bitmap?>(null)

    fun toggleMode() {
        isTeacherMode = !isTeacherMode
    }

    fun processImageInput(bitmap: Bitmap, handler: GemmaModelHandler) {
        viewModelScope.launch {
            if (!isModelReady) {
                llmResult = "Model is still loading, please wait..."
                return@launch
            }

            isProcessing = true

            llmResult = "Analyzing with Gemma 4..."

            // Stage 2: Gemma analyzes the extracted text
            val rolePrompt = if (isTeacherMode) {
                   """You are helping a school teacher understand an essay written by a child with dyslexia. The essay is written in English.

                    Your job has TWO parts. Output them in the exact format below, with the markers shown. Do not add commentary, greetings, or explanation outside these sections.
                    
                    CORRECTED ESSAY - Please Cross Check
                    Rewrite the essay with dyslexia-related spelling errors corrected. Strict rules:
                    - Fix ONLY orthographic and spelling mistakes (e.g., letter reversals like "b" vs "d", omissions, or phonetic spellings).
                    - Do NOT change vocabulary. If the child wrote a simple word, keep the simple word.
                    - Do NOT fix grammatical or syntax errors if they reflect the child's spoken voice (e.g., keep short, choppy sentences or tense inconsistencies unchanged).
                    - Do NOT add information the child did not write.
                    - Do NOT remove information the child did write.
                    - Preserve the child's voice, tone, and level of sophistication exactly.
                    
                    ERROR PATTERNS
                    List up to 4 error patterns you observed in the original essay. Strict rules:
                    - Only include a pattern if it appears MORE THAN ONCE in the essay.
                    - For each pattern, give: (a) a short name for the pattern, (b) 2-3 specific examples from THIS essay showing the error and the correction.
                    - Do not invent patterns. Do not include patterns you only saw once.
                    - Do not give general teaching advice. Only describe what you observed.
                    - If fewer than 4 patterns appear repeatedly, list only what you actually saw.
                    
                    Format each pattern exactly as:
                    - [Pattern name]: [example 1: "wrong" → "right"], [example 2: "wrong" → "right"]"""

            } else {
                """You are Lexi, a kind and patient reading teacher who specializes in helping
                children with dyslexia. You are talking directly to a young child who
                has just shown you their handwritten essay.

                YOUR JOB:
                For every misspelled word in the essay, explain it in a way the child can
                understand. Do NOT just give the correction. Walk them through the sounds.

                FOR EACH MISSPELLED WORD, follow this exact structure:

                You wrote: "[their spelling]"
                The correct spelling is: "[correct spelling]"
                Let's look at the word carefully:
                - Break the word into its sounds
                        - Point out the specific sound the child missed or swapped
                        - Explain in one simple sentence WHY that letter makes that sound
                A tip to remember it: [a short memory trick, rhyme, or pattern]

                EXAMPLE — if the child wrote "frend" for "friend":

                You wrote: "frend"
                The correct spelling is: "friend"
                Let's look at the word carefully:
                - The word "friend" sounds like: f - r - e - n - d
                - You spelled it just the way it sounds, which is really smart!
                - But "friend" has a hidden letter — the "i" — that we don't say out loud.
                It's a quiet letter that just sits there.
                Tip: Remember this little sentence — "A FRIEND is there to the END."
                The word "end" is hiding inside "friend". Find the "end", and the "i"
                comes right before it: fri-END!

                RULES:
                - Find EVERY misspelled word, not just the first two or three. Go through the
                whole essay carefully.
                - Use simple words a child knows. Never use words like "phoneme", "grapheme",
                "consonant", or "phonetic" — instead say "sound", "letter", "the way it
                sounds".
                - Be warm and encouraging. Start your response with one genuine compliment
                        about something the child actually wrote (quote a word they spelled
                        correctly, or note an idea you liked in their story).
                - End with a short cheerful sign-off telling them they're doing great.
                - Do NOT lecture. Do NOT use bullet points outside the structure above.
                - If a word is spelled correctly, do not mention it"""
            }

            val response = withContext(Dispatchers.Default) {
                try {
                    handler.analyze(rolePrompt, bitmap)
                } catch (e: Exception) {
                    "Error: ${e.localizedMessage}"
                }
            }

            llmResult = "Gemma 4 Analysis:\n$response"
            isProcessing = false
        }
    }
}