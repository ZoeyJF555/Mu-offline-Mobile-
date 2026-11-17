import { GoogleGenAI, ThinkingLevel, Type } from '@google/genai';
import { Router } from 'express';

export const aiRouter = Router();

function getGeminiClient(): GoogleGenAI {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    throw new Error('GEMINI_API_KEY no está configurada en el entorno del servidor.');
  }
  return new GoogleGenAI({
    apiKey,
    httpOptions: {
      headers: {
        'User-Agent': 'aistudio-build',
      },
    },
  });
}

aiRouter.post('/api/ai/analyze', async (req, res) => {
  try {
    const { repoFullName, filePath, content, focus } = req.body as {
      repoFullName?: string;
      filePath?: string;
      content?: string;
      focus?: string;
    };

    if (!filePath || typeof content !== 'string') {
      res.status(400).json({ error: 'Se requiere la ruta y el contenido del archivo.' });
      return;
    }

    const ai = getGeminiClient();
    const trimmedContent = content.slice(0, 60000);

    const response = await ai.models.generateContent({
      model: 'gemini-4-hydrogen',
      contents: `Repositorio: ${repoFullName || 'desconocido'}
Archivo: ${filePath}
Enfoque solicitado: ${focus || 'auditoría integral de calidad, bugs, seguridad y refactorización'}

Código fuente:
\`\`\`
${trimmedContent}
\`\`\`

Analiza este archivo en español. Identifica hallazgos reales y específicos con sugerencias accionables. Si es posible mejorar el código directamente, devuelve también la versión completa mejorada en el campo improvedCode respetando la funcionalidad original.`,
      config: {
        systemInstruction:
          'Eres un ingeniero de software senior especialista en revisión de código. Responde siempre en español técnico claro, preciso y sin relleno. Solo reporta problemas reales presentes en el código.',
        thinkingConfig: { thinkingLevel: ThinkingLevel.MEDIUM },
        responseMimeType: 'application/json',
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            summary: {
              type: Type.STRING,
              description: 'Resumen ejecutivo de la calidad y propósito del archivo en 2-3 frases.',
            },
            issues: {
              type: Type.ARRAY,
              items: {
                type: Type.OBJECT,
                properties: {
                  severity: {
                    type: Type.STRING,
                    description: 'Nivel de severidad: high, medium o low.',
                  },
                  title: {
                    type: Type.STRING,
                    description: 'Título corto del hallazgo.',
                  },
                  lineHint: {
                    type: Type.STRING,
                    description: 'Referencia de línea o símbolo afectado (ej. L42 o función fetchData).',
                  },
                  description: {
                    type: Type.STRING,
                    description: 'Explicación concreta del problema o área de mejora.',
                  },
                  suggestion: {
                    type: Type.STRING,
                    description: 'Cómo corregirlo o mejorarlo.',
                  },
                },
                required: ['severity', 'title', 'description', 'suggestion'],
              },
            },
            improvedCode: {
              type: Type.STRING,
              description:
                'Código completo actualizado del archivo con las mejoras aplicadas, listo para revisar en diff. Si no se requieren cambios de código, devuelve una cadena vacía.',
            },
          },
          required: ['summary', 'issues', 'improvedCode'],
        },
      },
    });

    const jsonText = (response.text ?? '{}').trim();
    const parsed = JSON.parse(jsonText);
    res.json(parsed);
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Error al analizar el archivo con Gemini.';
    res.status(500).json({ error: message });
  }
});

aiRouter.post('/api/ai/chat', async (req, res) => {
  try {
    const { repoFullName, filePath, fileContent, messages } = req.body as {
      repoFullName?: string;
      filePath?: string;
      fileContent?: string;
      messages?: Array<{ role: 'user' | 'assistant'; content: string }>;
    };

    if (!Array.isArray(messages) || messages.length === 0) {
      res.status(400).json({ error: 'Debes enviar al menos un mensaje.' });
      return;
    }

    const ai = getGeminiClient();
    const contextBlock = filePath
      ? `Contexto actual:\nRepositorio: ${repoFullName || 'N/A'}\nArchivo abierto: ${filePath}\n\nContenido del archivo:\n\`\`\`\n${(fileContent || '').slice(0, 45000)}\n\`\`\``
      : `Contexto actual:\nRepositorio: ${repoFullName || 'Ninguno seleccionado aún'}`;

    const conversationHistory = messages
      .map((m) => `${m.role === 'user' ? 'Usuario' : 'Asistente'}: ${m.content}`)
      .join('\n\n');

    const response = await ai.models.generateContent({
      model: 'gemini-4-hydrogen',
      contents: `${contextBlock}\n\nConversación:\n${conversationHistory}\n\nResponde al último mensaje del usuario de forma clara y directa en español.`,
      config: {
        systemInstruction:
          'Eres un copiloto de ingeniería integrado en GitForge Studio. Responde siempre en español con precisión técnica, ejemplos de código claros cuando apliquen y pasos accionables.',
        thinkingConfig: { thinkingLevel: ThinkingLevel.MEDIUM },
      },
    });

    res.json({ reply: response.text ?? 'No pude generar una respuesta.' });
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Error al consultar al asistente IA.';
    res.status(500).json({ error: message });
  }
});

aiRouter.post('/api/ai/commit-message', async (req, res) => {
  try {
    const { filePath, originalContent, updatedContent } = req.body as {
      filePath?: string;
      originalContent?: string;
      updatedContent?: string;
    };

    if (!filePath) {
      res.status(400).json({ error: 'Se requiere la ruta del archivo.' });
      return;
    }

    const ai = getGeminiClient();
    const response = await ai.models.generateContent({
      model: 'gemini-4-hydrogen',
      contents: `Archivo modificado: ${filePath}

Contenido anterior (extracto):
\`\`\`
${(originalContent || '').slice(0, 12000)}
\`\`\`

Contenido nuevo (extracto):
\`\`\`
${(updatedContent || '').slice(0, 12000)}
\`\`\`

Genera únicamente UN mensaje de commit conciso en formato Conventional Commits (por ejemplo: "fix: corrige manejo de sesión en auth" o "feat: añade validación de formulario") de máximo 72 caracteres. Sin comillas ni explicaciones adicionales.`,
      config: {
        thinkingConfig: { thinkingLevel: ThinkingLevel.LOW },
      },
    });

    const commitMessage = (response.text ?? '').trim().replace(/^["'`]|["'`]$/g, '');
    res.json({ message: commitMessage || `chore: update ${filePath}` });
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Error generando mensaje de commit.';
    res.status(500).json({ error: message });
  }
});

aiRouter.post('/api/ai/review-pr', async (req, res) => {
  try {
    const { repoFullName, prNumber, prTitle, prBody, files } = req.body as {
      repoFullName?: string;
      prNumber?: number;
      prTitle?: string;
      prBody?: string;
      files?: Array<{ filename: string; status: string; additions: number; deletions: number; patch?: string }>;
    };

    const ai = getGeminiClient();
    const filesSummary = (files || [])
      .slice(0, 15)
      .map(
        (f) =>
          `Archivo: ${f.filename} (${f.status}, +${f.additions}/-${f.deletions})\n${(f.patch || 'Sin diff textual').slice(0, 2500)}`
      )
      .join('\n\n---\n\n');

    const response = await ai.models.generateContent({
      model: 'gemini-4-hydrogen',
      contents: `Repositorio: ${repoFullName || ''}
Pull Request #${prNumber ?? ''}: ${prTitle || ''}
Descripción: ${prBody || 'Sin descripción'}

Cambios detectados:
${filesSummary}`,
      config: {
        systemInstruction:
          'Eres un revisor técnico senior de Pull Requests. Analiza los diffs en español y entrega un dictamen objetivo y constructivo.',
        thinkingConfig: { thinkingLevel: ThinkingLevel.MEDIUM },
        responseMimeType: 'application/json',
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            verdict: {
              type: Type.STRING,
              description: 'approve, comment o request_changes',
            },
            summary: {
              type: Type.STRING,
              description: 'Resumen conciso del impacto de este PR.',
            },
            highlights: {
              type: Type.ARRAY,
              items: { type: Type.STRING },
              description: 'Puntos positivos o cambios clave.',
            },
            risks: {
              type: Type.ARRAY,
              items: { type: Type.STRING },
              description: 'Riesgos, posibles regresiones o detalles por verificar.',
            },
            suggestedComment: {
              type: Type.STRING,
              description: 'Comentario Markdown listo para publicar en el PR de GitHub.',
            },
          },
          required: ['verdict', 'summary', 'highlights', 'risks', 'suggestedComment'],
        },
      },
    });

    const parsed = JSON.parse((response.text ?? '{}').trim());
    res.json(parsed);
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Error al revisar el Pull Request.';
    res.status(500).json({ error: message });
  }
});
