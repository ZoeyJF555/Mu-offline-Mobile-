package com.muoffline.mobile.ui.game

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.muoffline.mobile.model.*
import kotlin.math.*

@Composable
fun GameWorldCanvas(
    hero: HeroSave,
    heroFacingAngle: Float,
    mapMeta: MapMeta,
    npcsOnMap: List<MapNpcDef>,
    monsters: List<LiveMonster>,
    drops: List<GroundDrop>,
    floatingTexts: List<FloatingText>,
    vfxList: List<SkillVfx>,
    lanPlayers: List<LanPlayer>,
    lanBosses: List<LanBossState>,
    hdGraphics: Boolean,
    animationsEnabled: Boolean,
    onTapNpc: (MapNpcDef) -> Unit,
    onTapWorld: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val worldZoom = remember(density) { (density * 0.68f).coerceIn(1.35f, 2.15f) }

    val latestHero by rememberUpdatedState(hero)
    val latestNpcs by rememberUpdatedState(npcsOnMap)
    val latestZoom by rememberUpdatedState(worldZoom)
    val latestOnTapNpc by rememberUpdatedState(onTapNpc)
    val latestOnTapWorld by rememberUpdatedState(onTapWorld)

    var frameClockMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(animationsEnabled) {
        while (true) {
            withFrameMillis {
                frameClockMs = System.currentTimeMillis()
            }
            if (!animationsEnabled) {
                kotlinx.coroutines.delay(90L)
            }
        }
    }

    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }
    val subLabelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
    }
    val pillPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.argb(185, 8, 11, 19)
        }
    }
    val floatPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(4f, 0f, 1.5f, android.graphics.Color.BLACK)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val currentHero = latestHero
                    val z = latestZoom
                    val wx = currentHero.x + (offset.x - size.width / 2f) / z
                    val wy = currentHero.y + (offset.y - size.height / 2f) / z
                    val npc = latestNpcs.firstOrNull {
                        hypot((it.wx - wx).toDouble(), (it.wy - wy).toDouble()) <= 38.0
                    }
                    if (npc != null) {
                        latestOnTapNpc(npc)
                        return@detectTapGestures
                    }
                    latestOnTapWorld(wx, wy)
                }
            }
    ) {
        val now = frameClockMs
        val viewW = size.width / worldZoom
        val viewH = size.height / worldZoom
        val camX = hero.x - viewW / 2f
        val camY = hero.y - viewH / 2f

        // Base backdrop behind zoomed viewport
        drawRect(color = Color(0xFF07090E))

        withTransform({
            scale(scaleX = worldZoom, scaleY = worldZoom, pivot = Offset.Zero)
        }) {
            val primaryColor = Color(mapMeta.groundPrimary)
            val secondaryColor = Color(mapMeta.groundSecondary)
            val accentColor = Color(mapMeta.accentColor)
            val tileSize = if (hdGraphics) 60f else 84f

            val startTileX = floor(camX / tileSize).toInt() - 1
            val endTileX = ceil((camX + viewW) / tileSize).toInt() + 1
            val startTileY = floor(camY / tileSize).toInt() - 1
            val endTileY = ceil((camY + viewH) / tileSize).toInt() + 1

            // 1. 2.5D Isometric Flagstone / Terrain Grid
            for (tx in startTileX..endTileX) {
                for (ty in startTileY..endTileY) {
                    val wx = tx * tileSize
                    val wy = ty * tileSize
                    val sx = wx - camX
                    val sy = wy - camY
                    val inWorld = wx >= 0f && wy >= 0f && wx < MuWorldConstants.WORLD_SIZE && wy < MuWorldConstants.WORLD_SIZE
                    if (!inWorld) {
                        // Outer abyss cliff stones rather than flat void
                        val outerShade = if (((tx * 7 + ty * 13) and 1) == 0) Color(0xFF0B0E16) else Color(0xFF0E131E)
                        drawRect(color = outerShade, topLeft = Offset(sx, sy), size = Size(tileSize + 0.8f, tileSize + 0.8f))
                        continue
                    }

                    val hash = abs((tx * 73856093) xor (ty * 19349663))
                    val inSafe = hypot(
                        (wx + tileSize / 2f - MuWorldConstants.SAFE_CENTER).toDouble(),
                        (wy + tileSize / 2f - MuWorldConstants.SAFE_CENTER).toDouble()
                    ) <= MuWorldConstants.SAFE_RADIUS

                    val baseFill = when {
                        inSafe -> if (((tx + ty) and 1) == 0) Color(mapMeta.safeZoneColor) else Color(mapMeta.safeZoneColor).copy(alpha = 0.82f)
                        (hash % 5) == 0 -> secondaryColor.copy(alpha = 0.92f)
                        else -> primaryColor
                    }
                    drawRect(color = baseFill, topLeft = Offset(sx, sy), size = Size(tileSize + 0.6f, tileSize + 0.6f))

                    // Beveled isometric diamond stone inset
                    if (hdGraphics) {
                        val cx = sx + tileSize * 0.5f
                        val cy = sy + tileSize * 0.5f
                        val r = tileSize * 0.44f
                        val diamond = Path().apply {
                            moveTo(cx, cy - r)
                            lineTo(cx + r, cy)
                            lineTo(cx, cy + r)
                            lineTo(cx - r, cy)
                            close()
                        }
                        drawPath(
                            path = diamond,
                            color = (if (inSafe) Color(0xFFFDE68A) else Color.White).copy(alpha = if (inSafe) 0.08f else 0.035f)
                        )
                        drawPath(
                            path = diamond,
                            color = Color.Black.copy(alpha = 0.24f),
                            style = Stroke(width = 1f)
                        )
                    }

                    // Deterministic natural MU props outside the safe plaza center
                    if (!inSafe && (hash % 11) == 0) {
                        drawMapEnvironmentProp(
                            mapId = hero.mapId,
                            cx = sx + tileSize * 0.5f,
                            cy = sy + tileSize * 0.5f,
                            accent = accentColor,
                            variant = hash % 3
                        )
                    }
                }
            }

            // World perimeter stone wall
            drawRect(
                color = accentColor.copy(alpha = 0.65f),
                topLeft = Offset(-camX, -camY),
                size = Size(MuWorldConstants.WORLD_SIZE, MuWorldConstants.WORLD_SIZE),
                style = Stroke(width = 4f)
            )

            // 2. Citadel Safe Plaza, Roads, Walls & Central Fountain
            val safeSx = MuWorldConstants.SAFE_CENTER - camX
            val safeSy = MuWorldConstants.SAFE_CENTER - camY
            val safeR = MuWorldConstants.SAFE_RADIUS

            // Cross cobblestone avenues extending outward
            val roadColor = Color(0xFF3E362B)
            val roadTrim = Color(0xFF6B5D4D)
            drawRect(color = roadColor, topLeft = Offset(safeSx - 20f, safeSy - 240f), size = Size(40f, 480f))
            drawRect(color = roadColor, topLeft = Offset(safeSx - 240f, safeSy - 20f), size = Size(480f, 40f))
            drawRect(color = roadTrim, topLeft = Offset(safeSx - 20f, safeSy - 240f), size = Size(40f, 480f), style = Stroke(width = 1.5f))
            drawRect(color = roadTrim, topLeft = Offset(safeSx - 240f, safeSy - 20f), size = Size(480f, 40f), style = Stroke(width = 1.5f))

            // Safe citadel outer ring & corner gate pillars
            drawCircle(color = accentColor.copy(alpha = 0.22f), radius = safeR + 6f, center = Offset(safeSx, safeSy), style = Stroke(width = 10f))
            drawCircle(color = accentColor.copy(alpha = 0.78f), radius = safeR, center = Offset(safeSx, safeSy), style = Stroke(width = 2.5f))

            // 4 medieval corner stone pillars + braziers around Safe Plaza
            val gateAngles = listOf(45.0, 135.0, 225.0, 315.0)
            gateAngles.forEachIndexed { idx, deg ->
                val rad = Math.toRadians(deg)
                val gx = safeSx + (cos(rad) * (safeR - 6f)).toFloat()
                val gy = safeSy + (sin(rad) * (safeR - 6f)).toFloat()
                drawRoundRect(
                    color = Color(0xFF475569),
                    topLeft = Offset(gx - 9f, gy - 14f),
                    size = Size(18f, 22f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                val flicker = if (animationsEnabled) 1f + sin(now / 120.0 + idx).toFloat() * 0.16f else 1f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFF59E0B).copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(gx, gy - 12f),
                        radius = 26f * flicker
                    ),
                    radius = 26f * flicker,
                    center = Offset(gx, gy - 12f)
                )
                drawCircle(color = Color(0xFFFDE047), radius = 4f * flicker, center = Offset(gx, gy - 12f))
            }

            // Lorencia southern river & iconic 3D-shaded wooden bridge (OpenMu-Client-Babylon landmark)
            if (hero.mapId == MapId.LORENCIA) {
                val riverTop = 752f - camY
                val riverH = 54f
                drawRect(color = Color(0xFF0C3B5E), topLeft = Offset(-camX, riverTop), size = Size(MuWorldConstants.WORLD_SIZE, riverH))
                drawRect(color = Color(0xFF0284C7).copy(alpha = 0.45f), topLeft = Offset(-camX, riverTop + 8f), size = Size(MuWorldConstants.WORLD_SIZE, riverH - 16f))
                val waveShift = if (animationsEnabled) ((now % 2400L) / 2400f) * 48f else 0f
                var rx = -camX + waveShift
                while (rx < viewW + 40f) {
                    drawLine(Color(0xFF7DD3FC).copy(alpha = 0.32f), Offset(rx, riverTop + 18f), Offset(rx + 20f, riverTop + 18f), strokeWidth = 2f)
                    drawLine(Color(0xFF38BDF8).copy(alpha = 0.25f), Offset(rx + 12f, riverTop + 34f), Offset(rx + 34f, riverTop + 34f), strokeWidth = 2f)
                    rx += 68f
                }
                // Wooden bridge crossing at X=446..514
                val bx = 446f - camX
                val bw = 68f
                drawRoundRect(color = Color(0xFF5C3A21), topLeft = Offset(bx, riverTop - 4f), size = Size(bw, riverH + 8f), cornerRadius = CornerRadius(4f, 4f))
                for (plank in 0..7) {
                    val py = riverTop + plank * 7.5f
                    drawLine(Color(0xFF3E2412), Offset(bx + 2f, py), Offset(bx + bw - 2f, py), strokeWidth = 1.6f)
                }
                drawRect(color = Color(0xFF8B5A2B), topLeft = Offset(bx, riverTop - 4f), size = Size(6f, riverH + 8f))
                drawRect(color = Color(0xFF8B5A2B), topLeft = Offset(bx + bw - 6f, riverTop - 4f), size = Size(6f, riverH + 8f))
            }

            // Central Safe Zone Magic Fountain / Warp Crystal
            drawCircle(color = Color(0xFF334155), radius = 26f, center = Offset(safeSx, safeSy))
            drawCircle(color = Color(0xFF64748B), radius = 26f, center = Offset(safeSx, safeSy), style = Stroke(width = 2.5f))
            val ripple = if (animationsEnabled) 10f + ((now % 1400L) / 1400f) * 14f else 14f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(accentColor, Color(0xFF0F172A)),
                    center = Offset(safeSx, safeSy),
                    radius = 18f
                ),
                radius = 18f,
                center = Offset(safeSx, safeSy)
            )
            drawCircle(color = Color.White.copy(alpha = 0.55f), radius = ripple, center = Offset(safeSx, safeSy), style = Stroke(width = 1.5f))

            // 3. NPCs with MU stalls, anvils, Chaos Goblin cauldron, vault chests & crisp pills
            for (npc in npcsOnMap) {
                val nx = npc.wx - camX
                val ny = npc.wy - camY
                if (nx < -90f || nx > viewW + 90f || ny < -90f || ny > viewH + 90f) continue
                drawMuNpc(
                    npc = npc,
                    nx = nx,
                    ny = ny,
                    now = now,
                    animationsEnabled = animationsEnabled,
                    labelPaint = labelPaint,
                    subLabelPaint = subLabelPaint,
                    pillPaint = pillPaint
                )
            }

            // 4. Ground Drops (Zen piles & glowing Excellent/Jewel boxes)
            for (drop in drops) {
                val dx = drop.x - camX
                val dy = drop.y - camY
                if (dx < -40f || dx > viewW + 40f || dy < -40f || dy > viewH + 40f) continue
                val pulse = if (animationsEnabled) 1f + sin(now / 170.0).toFloat() * 0.14f else 1f
                if (drop.zen != null) {
                    drawCircle(color = Color(0xFFFBBF24).copy(alpha = 0.25f), radius = 13f * pulse, center = Offset(dx, dy))
                    drawCircle(color = Color(0xFFF59E0B), radius = 5.5f, center = Offset(dx - 3f, dy + 1f))
                    drawCircle(color = Color(0xFFFDE047), radius = 5.5f, center = Offset(dx + 3f, dy - 1f))
                    drawPillLabel("${drop.zen} Zen", dx, dy - 12f, Color(0xFFFDE68A).toArgb(), subLabelPaint, pillPaint)
                } else if (drop.item != null) {
                    val c = Color(drop.item.colorHex)
                    drawCircle(color = c.copy(alpha = 0.30f), radius = 15f * pulse, center = Offset(dx, dy))
                    drawRoundRect(
                        color = c,
                        topLeft = Offset(dx - 6f, dy - 6f),
                        size = Size(12f, 12f),
                        cornerRadius = CornerRadius(2.5f, 2.5f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.65f),
                        topLeft = Offset(dx - 6f, dy - 6f),
                        size = Size(12f, 12f),
                        cornerRadius = CornerRadius(2.5f, 2.5f),
                        style = Stroke(width = 1f)
                    )
                    drawPillLabel(drop.item.name, dx, dy - 14f, c.toArgb(), subLabelPaint, pillPaint)
                }
            }

            // 5. Monsters with authentic MU silhouettes (NO smiley faces!)
            for (m in monsters) {
                if (m.hp <= 0) continue
                val mx = m.x - camX
                val my = m.y - camY
                if (mx < -80f || mx > viewW + 80f || my < -80f || my > viewH + 80f) continue
                drawMuMonster(
                    m = m,
                    mx = mx,
                    my = my,
                    now = now,
                    animationsEnabled = animationsEnabled,
                    subLabelPaint = subLabelPaint,
                    pillPaint = pillPaint
                )
            }

            // 6. Cooperative LAN Bosses
            for (lb in lanBosses) {
                if (lb.mapId != hero.mapId || lb.hp <= 0) continue
                val bx = lb.x - camX
                val by = lb.y - camY
                val pulse = if (animationsEnabled) 26f + sin(now / 150.0).toFloat() * 4f else 26f
                drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.32f), radius = pulse + 9f, center = Offset(bx, by), style = Stroke(width = 3f))
                drawCircle(color = Color(0xFF0284C7), radius = 24f, center = Offset(bx, by))
                drawCircle(color = Color(0xFFFBBF24), radius = 24f, center = Offset(bx, by), style = Stroke(width = 2.5f))
                val ratio = (lb.hp.toFloat() / max(1, lb.maxHp)).coerceIn(0f, 1f)
                drawRect(color = Color(0xFF0F172A), topLeft = Offset(bx - 32f, by - 36f), size = Size(64f, 6f))
                drawRect(color = Color(0xFF38BDF8), topLeft = Offset(bx - 32f, by - 36f), size = Size(64f * ratio, 6f))
                drawPillLabel("[BOSS LAN] ${lb.name}", bx, by - 42f, Color(0xFF38BDF8).toArgb(), labelPaint, pillPaint)
            }

            // 7. Remote LAN Players
            for (lp in lanPlayers) {
                if (lp.id == hero.id || lp.mapId != hero.mapId) continue
                val px = lp.x - camX
                val py = lp.y - camY
                drawHeroAvatar(
                    x = px,
                    y = py,
                    classId = lp.classId,
                    facingAngle = lp.dir,
                    wingsTier = lp.wingsTier,
                    weaponPlus = lp.weaponPlus,
                    hasPet = false,
                    hp = lp.hp,
                    maxHp = lp.maxHp,
                    displayName = "${lp.name} Nv.${lp.level}",
                    nowMs = now,
                    animationsEnabled = animationsEnabled,
                    isLocal = false,
                    labelPaint = labelPaint,
                    pillPaint = pillPaint
                )
            }

            // 8. Local Hero Avatar (centered in zoomed world)
            val heroWingsTier = hero.equipment.wings?.wingsTier?.coerceAtLeast(1) ?: 0
            drawHeroAvatar(
                x = viewW / 2f,
                y = viewH / 2f,
                classId = hero.classId,
                facingAngle = heroFacingAngle,
                wingsTier = heroWingsTier,
                weaponPlus = hero.equipment.weapon?.plus ?: 0,
                hasPet = hero.equipment.pet != null,
                hp = hero.hp,
                maxHp = hero.maxHp,
                displayName = "${hero.name} [Nv.${hero.level}]",
                nowMs = now,
                animationsEnabled = animationsEnabled,
                isLocal = true,
                labelPaint = labelPaint,
                pillPaint = pillPaint
            )

            // 9. Rich MU Online Skill VFX + Ambient Weather Particles (Devias Snow / Noria Spores)
            if (animationsEnabled) {
                for (vfx in vfxList) {
                    drawMuSkillVfx(vfx, camX, camY, now, hdGraphics)
                }
                if (hdGraphics && (hero.mapId == MapId.DEVIAS || hero.mapId == MapId.NORIA || hero.mapId == MapId.ICARUS)) {
                    val pColor = if (hero.mapId == MapId.NORIA) Color(0xFFA7F3D0) else Color(0xFFE0F2FE)
                    for (p in 0 until 18) {
                        val px = ((p * 97 + (now / 28L)) % max(1f, viewW).toInt()).toFloat()
                        val py = ((p * 61 + (now / 18L)) % max(1f, viewH).toInt()).toFloat()
                        drawCircle(color = pColor.copy(alpha = 0.45f), radius = if (hero.mapId == MapId.NORIA) 2.2f else 2.5f, center = Offset(px, py))
                    }
                }
            }

            // 10. Floating Damage / Loot Text
            drawIntoCanvas { canvas ->
                for (ft in floatingTexts) {
                    val age = (now - ft.createdAt).coerceAtLeast(0L)
                    val p = (age / 950f).coerceIn(0f, 1f)
                    val fx = ft.x - camX
                    val fy = (ft.y - camY) - p * 28f
                    val alphaInt = ((1f - p) * 255).toInt().coerceIn(0, 255)
                    val baseColor = Color(ft.colorHex)
                    floatPaint.textSize = if (ft.isCrit) 16f else 13f
                    floatPaint.color = android.graphics.Color.argb(
                        alphaInt,
                        (baseColor.red * 255).toInt(),
                        (baseColor.green * 255).toInt(),
                        (baseColor.blue * 255).toInt()
                    )
                    canvas.nativeCanvas.drawText(ft.text, fx, fy, floatPaint)
                }
            }
        }
    }
}

private fun DrawScope.drawPillLabel(
    text: String,
    cx: Float,
    baselineY: Float,
    textColorArgb: Int,
    paint: Paint,
    pillPaint: Paint
) {
    drawIntoCanvas { canvas ->
        val w = paint.measureText(text)
        val padX = 6f
        val top = baselineY - paint.textSize + 1f
        val bottom = baselineY + 4f
        canvas.nativeCanvas.drawRoundRect(
            RectF(cx - w / 2f - padX, top, cx + w / 2f + padX, bottom),
            6f,
            6f,
            pillPaint
        )
        paint.color = textColorArgb
        canvas.nativeCanvas.drawText(text, cx, baselineY, paint)
    }
}

private fun DrawScope.drawMapEnvironmentProp(
    mapId: MapId,
    cx: Float,
    cy: Float,
    accent: Color,
    variant: Int
) {
    when (mapId) {
        MapId.LORENCIA, MapId.NORIA, MapId.AIDA -> {
            // Medieval / Elven tree with shadow & canopy
            drawOval(color = Color.Black.copy(alpha = 0.32f), topLeft = Offset(cx - 12f, cy + 4f), size = Size(24f, 10f))
            drawRect(color = Color(0xFF5C4033), topLeft = Offset(cx - 3f, cy - 6f), size = Size(6f, 14f))
            val leafColor = if (mapId == MapId.NORIA) Color(0xFF10B981) else if (mapId == MapId.AIDA) Color(0xFF22C55E) else Color(0xFF2F5E38)
            drawCircle(color = leafColor.copy(alpha = 0.85f), radius = 12f + variant * 2f, center = Offset(cx, cy - 12f))
            drawCircle(color = leafColor.copy(alpha = 0.65f), radius = 8f, center = Offset(cx - 5f, cy - 16f))
        }
        MapId.DEVIAS -> {
            // Snow-capped ice crystal spires
            drawOval(color = Color.Black.copy(alpha = 0.28f), topLeft = Offset(cx - 11f, cy + 4f), size = Size(22f, 8f))
            drawPath(Path().apply {
                moveTo(cx, cy - 22f)
                lineTo(cx + 9f, cy + 6f)
                lineTo(cx - 9f, cy + 6f)
                close()
            }, color = Color(0xFF7DD3FC).copy(alpha = 0.78f))
            drawPath(Path().apply {
                moveTo(cx, cy - 22f)
                lineTo(cx + 9f, cy + 6f)
                lineTo(cx, cy + 6f)
                close()
            }, color = Color(0xFFE0F2FE).copy(alpha = 0.55f))
        }
        MapId.DUNGEON1, MapId.DUNGEON2, MapId.DUNGEON3, MapId.LOSTTOWER, MapId.KALIMA -> {
            // Ruined stone pillars & torches
            drawOval(color = Color.Black.copy(alpha = 0.4f), topLeft = Offset(cx - 10f, cy + 4f), size = Size(20f, 8f))
            drawRoundRect(color = Color(0xFF3F3B4D), topLeft = Offset(cx - 6f, cy - 16f), size = Size(12f, 24f), cornerRadius = CornerRadius(2f, 2f))
            drawCircle(color = accent.copy(alpha = 0.55f), radius = 3.5f, center = Offset(cx, cy - 17f))
        }
        MapId.ATLANS -> {
            // Underwater coral & glowing bubbles
            drawCircle(color = Color(0xFF0EA5E9).copy(alpha = 0.3f), radius = 10f, center = Offset(cx, cy))
            drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.65f), radius = 4f, center = Offset(cx - 4f, cy - 8f))
            drawCircle(color = Color(0xFFF472B6).copy(alpha = 0.6f), radius = 4.5f, center = Offset(cx + 5f, cy - 4f))
        }
        else -> {
            // Desert rocks / Kanturu metal ruins
            drawOval(color = Color.Black.copy(alpha = 0.35f), topLeft = Offset(cx - 12f, cy + 3f), size = Size(24f, 9f))
            drawRoundRect(color = Color(0xFF475569), topLeft = Offset(cx - 9f, cy - 8f), size = Size(18f, 14f), cornerRadius = CornerRadius(4f, 4f))
            drawRoundRect(color = accent.copy(alpha = 0.35f), topLeft = Offset(cx - 6f, cy - 6f), size = Size(12f, 6f), cornerRadius = CornerRadius(2f, 2f))
        }
    }
}

private fun DrawScope.drawMuNpc(
    npc: MapNpcDef,
    nx: Float,
    ny: Float,
    now: Long,
    animationsEnabled: Boolean,
    labelPaint: Paint,
    subLabelPaint: Paint,
    pillPaint: Paint
) {
    val npcColor = when (npc.role) {
        "chaos" -> Color(0xFFFBBF24)
        "archer" -> Color(0xFF38BDF8)
        "vault" -> Color(0xFF94A3B8)
        "quest" -> Color(0xFFA855F7)
        else -> Color(0xFF10B981)
    }

    // Shadow & glowing interactive circle
    drawOval(color = Color.Black.copy(alpha = 0.45f), topLeft = Offset(nx - 18f, ny + 8f), size = Size(36f, 12f))
    drawCircle(color = npcColor.copy(alpha = 0.22f), radius = 24f, center = Offset(nx, ny))
    drawCircle(color = npcColor.copy(alpha = 0.55f), radius = 24f, center = Offset(nx, ny), style = Stroke(width = 1.5f))

    // Role-specific landmark prop next to NPC
    when (npc.role) {
        "chaos" -> {
            val swirl = if (animationsEnabled) (now % 1600L) / 1600f * 6.28f else 0f
            drawCircle(color = Color(0xFF311042), radius = 11f, center = Offset(nx + 16f, ny + 4f))
            drawCircle(color = Color(0xFFA855F7), radius = 11f, center = Offset(nx + 16f, ny + 4f), style = Stroke(width = 2f))
            drawCircle(color = Color(0xFFFDE047), radius = 4f, center = Offset(nx + 16f + cos(swirl) * 5f, ny + 4f + sin(swirl) * 5f))
        }
        "vault" -> {
            drawRoundRect(color = Color(0xFF78350F), topLeft = Offset(nx + 10f, ny - 2f), size = Size(14f, 11f), cornerRadius = CornerRadius(2f, 2f))
            drawRect(color = Color(0xFFFBBF24), topLeft = Offset(nx + 10f, ny + 2f), size = Size(14f, 2.5f))
        }
        "archer" -> {
            drawPath(Path().apply {
                moveTo(nx - 16f, ny - 4f)
                lineTo(nx - 22f, ny - 16f)
                lineTo(nx - 11f, ny - 9f)
                close()
            }, color = Color(0xFFFDE047).copy(alpha = 0.85f))
        }
        "quest", "helper" -> {
            drawCircle(color = Color(0xFFFDE047), radius = 6.5f, center = Offset(nx, ny - 28f))
            drawLine(Color(0xFF1E293B), Offset(nx, ny - 31f), Offset(nx, ny - 26f), strokeWidth = 2f)
        }
        else -> {
            // Market awning behind vendor + Hanzo Blacksmith Forge / Barmaid Tavern Barrels
            drawRoundRect(color = Color(0xFF3F2E21), topLeft = Offset(nx - 18f, ny - 16f), size = Size(36f, 8f), cornerRadius = CornerRadius(3f, 3f))
            drawRoundRect(color = npcColor.copy(alpha = 0.78f), topLeft = Offset(nx - 18f, ny - 16f), size = Size(36f, 4f), cornerRadius = CornerRadius(2f, 2f))
            if (npc.id == "barmaid") {
                drawCircle(color = Color(0xFF78350F), radius = 5.5f, center = Offset(nx - 16f, ny + 6f))
                drawCircle(color = Color(0xFFD97706), radius = 5.5f, center = Offset(nx - 16f, ny + 6f), style = Stroke(width = 1.4f))
            } else if (npc.id == "smith") {
                drawRoundRect(color = Color(0xFF475569), topLeft = Offset(nx + 12f, ny + 1f), size = Size(10f, 7f), cornerRadius = CornerRadius(2f, 2f))
                drawCircle(color = Color(0xFFF97316).copy(alpha = 0.7f), radius = 4f, center = Offset(nx + 17f, ny - 2f))
            }
        }
    }

    // NPC articulated robe, shoulders & head
    drawRoundRect(color = npcColor, topLeft = Offset(nx - 8f, ny - 5f), size = Size(16f, 16f), cornerRadius = CornerRadius(4f, 4f))
    drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(nx - 10f, ny - 5f), size = Size(20f, 5f), cornerRadius = CornerRadius(2f, 2f))
    drawCircle(color = Color(0xFFFDE68A), radius = 5.5f, center = Offset(nx, ny - 11f))

    drawPillLabel(npc.name, nx, ny - 21f, Color(0xFFFDE68A).toArgb(), labelPaint, pillPaint)
    drawPillLabel(npc.subtitle, nx, ny + 25f, Color(0xFFCBD5E1).toArgb(), subLabelPaint, pillPaint)
}

private fun DrawScope.drawMuMonster(
    m: LiveMonster,
    mx: Float,
    my: Float,
    now: Long,
    animationsEnabled: Boolean,
    subLabelPaint: Paint,
    pillPaint: Paint
) {
    val flashing = now < m.hitFlashUntil
    val bodyColor = if (flashing) Color.White else Color(m.colorHex)
    val darkShade = if (flashing) Color.White else bodyColor.copy(alpha = 0.55f)
    val r = m.radius * 0.92f
    val bob = if (animationsEnabled) sin(now / 190.0 + m.homeX).toFloat() * 1.8f else 0f
    val cy = my + bob

    if (m.isBoss) {
        drawCircle(color = Color(0xFFFBBF24).copy(alpha = 0.34f), radius = r + 9f, center = Offset(mx, my), style = Stroke(width = 2.5f))
    }
    drawOval(color = Color.Black.copy(alpha = 0.45f), topLeft = Offset(mx - r * 0.9f, my + r * 0.48f), size = Size(r * 1.8f, r * 0.65f))

    val lower = m.templateId.lowercase()
    when {
        lower.contains("spider") || lower.contains("scorpion") -> {
            // 8 articulated spider/scorpion legs + abdomen + thorax
            for (leg in -1..1) {
                val dy = leg * 4.5f
                drawLine(bodyColor, Offset(mx - r - 6f, cy + dy), Offset(mx + r + 6f, cy - dy), strokeWidth = 2.5f, cap = StrokeCap.Round)
            }
            drawOval(color = darkShade, topLeft = Offset(mx - r * 0.65f, cy - r * 0.75f), size = Size(r * 1.3f, r * 0.95f))
            drawCircle(color = bodyColor, radius = r * 0.52f, center = Offset(mx, cy + r * 0.15f))
            drawCircle(color = Color(0xFFEF4444), radius = 2f, center = Offset(mx - 3f, cy + r * 0.2f))
            drawCircle(color = Color(0xFFEF4444), radius = 2f, center = Offset(mx + 3f, cy + r * 0.2f))
        }
        lower.contains("dragon") || lower.contains("phoenix") || lower.contains("hydra") || lower.contains("bahamut") -> {
            val flap = if (animationsEnabled) sin(now / 150.0).toFloat() * 5f else 0f
            val span = r * 1.55f
            drawPath(Path().apply {
                moveTo(mx, cy - 2f)
                lineTo(mx - span, cy - r * 0.95f + flap)
                lineTo(mx - span * 0.55f, cy + r * 0.45f)
                close()
            }, color = bodyColor.copy(alpha = 0.78f))
            drawPath(Path().apply {
                moveTo(mx, cy - 2f)
                lineTo(mx + span, cy - r * 0.95f + flap)
                lineTo(mx + span * 0.55f, cy + r * 0.45f)
                close()
            }, color = bodyColor.copy(alpha = 0.78f))
            // Dragon body & horned head
            drawOval(color = darkShade, topLeft = Offset(mx - r * 0.6f, cy - r * 0.45f), size = Size(r * 1.2f, r * 1.15f))
            drawPath(Path().apply {
                moveTo(mx - 5f, cy - r * 0.6f)
                lineTo(mx, cy - r * 1.15f)
                lineTo(mx + 5f, cy - r * 0.6f)
                close()
            }, color = bodyColor)
            drawCircle(color = Color(0xFFFDE047), radius = 2.2f, center = Offset(mx - 3.5f, cy - r * 0.5f))
            drawCircle(color = Color(0xFFFDE047), radius = 2.2f, center = Offset(mx + 3.5f, cy - r * 0.5f))
        }
        lower.contains("bull") || lower.contains("balrog") || lower.contains("kundun") || lower.contains("zaikan") || lower.contains("balgass") -> {
            // Broad demon/minotaur armor + horns + weapon axe
            drawLine(Color(0xFFFDE68A), Offset(mx - 5f, cy - r * 0.65f), Offset(mx - 12f, cy - r * 1.25f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(Color(0xFFFDE68A), Offset(mx + 5f, cy - r * 0.65f), Offset(mx + 12f, cy - r * 1.25f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawRoundRect(color = darkShade, topLeft = Offset(mx - r * 0.75f, cy - r * 0.45f), size = Size(r * 1.5f, r * 1.1f), cornerRadius = CornerRadius(5f, 5f))
            drawCircle(color = bodyColor, radius = r * 0.48f, center = Offset(mx, cy - r * 0.5f))
            // Axe
            drawLine(Color(0xFFCBD5E1), Offset(mx + r * 0.6f, cy), Offset(mx + r * 1.15f, cy - r * 0.7f), strokeWidth = 3f)
            drawCircle(color = Color(0xFFEF4444), radius = 2.2f, center = Offset(mx - 3.5f, cy - r * 0.52f))
            drawCircle(color = Color(0xFFEF4444), radius = 2.2f, center = Offset(mx + 3.5f, cy - r * 0.52f))
        }
        lower.contains("skeleton") || lower.contains("death") || lower.contains("shadow") || lower.contains("gorgon") -> {
            // Undead warrior: armored torso, skull head, sword & shield
            drawRoundRect(color = darkShade, topLeft = Offset(mx - r * 0.55f, cy - r * 0.35f), size = Size(r * 1.1f, r * 0.95f), cornerRadius = CornerRadius(4f, 4f))
            drawCircle(color = Color(0xFFE2E8F0), radius = r * 0.4f, center = Offset(mx, cy - r * 0.6f))
            // Sword & Shield
            drawLine(Color(0xFFE2E8F0), Offset(mx + r * 0.5f, cy), Offset(mx + r * 0.95f, cy - r * 0.65f), strokeWidth = 2.5f)
            drawRoundRect(color = bodyColor, topLeft = Offset(mx - r * 0.95f, cy - r * 0.35f), size = Size(6f, 11f), cornerRadius = CornerRadius(2f, 2f))
        }
        lower.contains("hound") -> {
            // Four-legged Lorencia Hound / Wolf silhouette
            drawOval(color = darkShade, topLeft = Offset(mx - r * 0.9f, cy - r * 0.35f), size = Size(r * 1.45f, r * 0.75f))
            drawLine(bodyColor, Offset(mx - r * 0.55f, cy + 2f), Offset(mx - r * 0.65f, cy + r * 0.65f), strokeWidth = 2.6f, cap = StrokeCap.Round)
            drawLine(bodyColor, Offset(mx + r * 0.25f, cy + 2f), Offset(mx + r * 0.35f, cy + r * 0.65f), strokeWidth = 2.6f, cap = StrokeCap.Round)
            drawCircle(color = bodyColor, radius = r * 0.42f, center = Offset(mx + r * 0.55f, cy - r * 0.25f))
            drawPath(Path().apply {
                moveTo(mx + r * 0.35f, cy - r * 0.55f)
                lineTo(mx + r * 0.45f, cy - r * 0.95f)
                lineTo(mx + r * 0.65f, cy - r * 0.55f)
                close()
            }, color = darkShade)
            drawCircle(color = Color(0xFFEF4444), radius = 2f, center = Offset(mx + r * 0.68f, cy - r * 0.3f))
        }
        else -> {
            // Golem / Goblin / Yeti / Mutant humanoid silhouette
            drawRoundRect(color = darkShade, topLeft = Offset(mx - r * 0.7f, cy - r * 0.35f), size = Size(r * 1.4f, r * 0.95f), cornerRadius = CornerRadius(6f, 6f))
            drawCircle(color = bodyColor, radius = r * 0.45f, center = Offset(mx, cy - r * 0.55f))
            drawLine(Color(0xFF94A3B8), Offset(mx + r * 0.55f, cy + 2f), Offset(mx + r * 1.0f, cy - r * 0.4f), strokeWidth = 3f)
        }
    }

    // Crisp HP bar & pill name
    val barW = max(36f, r * 2.2f)
    val barTop = cy - r - 15f
    drawRect(color = Color(0xFF0F172A), topLeft = Offset(mx - barW / 2f, barTop), size = Size(barW, 4.5f))
    val hpRatio = (m.hp.toFloat() / max(1, m.maxHp)).coerceIn(0f, 1f)
    drawRect(
        color = if (m.isBoss) Color(0xFFF59E0B) else Color(0xFFEF4444),
        topLeft = Offset(mx - barW / 2f, barTop),
        size = Size(barW * hpRatio, 4.5f)
    )
    drawPillLabel(
        text = "${m.name} Nv.${m.level}",
        cx = mx,
        baselineY = barTop - 4f,
        textColorArgb = if (m.isBoss) Color(0xFFFBBF24).toArgb() else Color.White.toArgb(),
        paint = subLabelPaint,
        pillPaint = pillPaint
    )
}

private fun DrawScope.drawMuSkillVfx(
    vfx: SkillVfx,
    camX: Float,
    camY: Float,
    now: Long,
    hdGraphics: Boolean
) {
    val age = (now - vfx.createdAt).coerceAtLeast(0L)
    val progress = (age.toFloat() / max(1L, vfx.durationMs)).coerceIn(0f, 1f)
    val alpha = (1f - progress) * 0.88f
    val sx = vfx.x - camX
    val sy = vfx.y - camY
    val tx = vfx.targetX - camX
    val ty = vfx.targetY - camY
    val c = Color(vfx.colorHex).copy(alpha = alpha)

    when (vfx.skillId) {
        "evil-spirit" -> {
            val r = vfx.radius * (0.4f + progress * 0.65f)
            drawCircle(color = c.copy(alpha = alpha * 0.25f), radius = r, center = Offset(sx, sy))
            for (spirit in 0 until 5) {
                val ang = progress * 8.5f + spirit * 1.256f
                val px = sx + cos(ang) * r * 0.78f
                val py = sy + sin(ang) * r * 0.78f
                drawCircle(color = Color(0xFF1E1B4B).copy(alpha = alpha), radius = 6.5f, center = Offset(px, py))
                drawCircle(color = Color(0xFF818CF8).copy(alpha = alpha), radius = 3.5f, center = Offset(px, py))
            }
        }
        "meteorite" -> {
            val fallY = ty - (1f - progress) * 110f
            drawLine(color = Color(0xFFF97316).copy(alpha = alpha), start = Offset(tx - 18f, fallY - 28f), end = Offset(tx, fallY), strokeWidth = 6f, cap = StrokeCap.Round)
            drawCircle(color = Color(0xFFFDE047).copy(alpha = alpha), radius = 10f, center = Offset(tx, fallY))
            if (progress > 0.45f) {
                drawCircle(color = c, radius = vfx.radius * (progress - 0.3f), center = Offset(tx, ty), style = Stroke(width = 4f))
            }
        }
        "ice-storm", "ice-arrow" -> {
            val r = vfx.radius * (0.4f + progress * 0.6f)
            drawCircle(color = Color(0xFF38BDF8).copy(alpha = alpha * 0.28f), radius = r, center = Offset(tx, ty))
            for (shard in 0 until 6) {
                val ang = shard * 1.047f + progress * 2f
                val ix = tx + cos(ang) * r * 0.55f
                val iy = ty + sin(ang) * r * 0.55f - (1f - progress) * 24f
                drawLine(Color(0xFFE0F2FE).copy(alpha = alpha), Offset(ix, iy - 10f), Offset(ix, iy + 6f), strokeWidth = 3f)
            }
        }
        "twisting-slash", "rageful-blow", "power-slash", "flame-strike", "chaotic-diseier" -> {
            val curRadius = vfx.radius * (0.35f + progress * 0.75f)
            drawCircle(color = c, radius = curRadius, center = Offset(sx, sy), style = Stroke(width = 5f))
            drawCircle(color = c.copy(alpha = alpha * 0.22f), radius = curRadius * 0.85f, center = Offset(sx, sy))
            if (hdGraphics) {
                for (orb in 0..3) {
                    val ang = progress * 7.5f + orb * 1.57f
                    drawCircle(color = Color.White.copy(alpha = alpha), radius = 4f, center = Offset(sx + cos(ang) * curRadius * 0.78f, sy + sin(ang) * curRadius * 0.78f))
                }
            }
        }
        "triple-shot" -> {
            val baseAngle = atan2(ty - sy, tx - sx)
            for (spread in listOf(-0.22f, 0f, 0.22f)) {
                val ex = sx + cos(baseAngle + spread) * vfx.radius * (0.5f + progress * 0.8f)
                val ey = sy + sin(baseAngle + spread) * vfx.radius * (0.5f + progress * 0.8f)
                drawLine(Color(0xFF34D399).copy(alpha = alpha), Offset(sx, sy), Offset(ex, ey), strokeWidth = 3f, cap = StrokeCap.Round)
            }
        }
        "heal-aura" -> {
            drawCircle(color = Color(0xFF34D399).copy(alpha = alpha * 0.35f), radius = vfx.radius * (0.4f + progress * 0.6f), center = Offset(sx, sy))
            drawCircle(color = Color(0xFFA7F3D0).copy(alpha = alpha), radius = vfx.radius * (0.4f + progress * 0.6f), center = Offset(sx, sy), style = Stroke(width = 2.5f))
        }
        else -> {
            drawLine(color = c, start = Offset(sx, sy), end = Offset(tx, ty), strokeWidth = 4.5f, cap = StrokeCap.Round)
            drawCircle(color = c.copy(alpha = alpha * 0.45f), radius = vfx.radius * progress, center = Offset(tx, ty))
        }
    }
}

private fun DrawScope.drawHeroAvatar(
    x: Float,
    y: Float,
    classId: ClassId,
    facingAngle: Float,
    wingsTier: Int,
    weaponPlus: Int,
    hasPet: Boolean,
    hp: Int,
    maxHp: Int,
    displayName: String,
    nowMs: Long,
    animationsEnabled: Boolean,
    isLocal: Boolean,
    labelPaint: Paint,
    pillPaint: Paint
) {
    val accent = Color(MuCatalog.CLASS_CATALOG.getValue(classId).accentColor)
    val armorTrim = when (classId) {
        ClassId.DK -> Color(0xFFFCA5A5)
        ClassId.DW -> Color(0xFF93C5FD)
        ClassId.ELF -> Color(0xFF6EE7B7)
        ClassId.MG -> Color(0xFFD8B4FE)
        ClassId.DL -> Color(0xFFFDE68A)
    }

    // +7 / +11 / +13 Excellence Jewel Aura
    if (weaponPlus >= 7) {
        val rot = if (animationsEnabled) (nowMs % 1600L) / 1600f * 6.283f else 0f
        val auraColor = if (weaponPlus >= 11) Color(0xFF38BDF8) else Color(0xFFFBBF24)
        drawCircle(color = auraColor.copy(alpha = 0.42f), radius = 24f, center = Offset(x, y), style = Stroke(width = 2.2f))
        drawCircle(color = auraColor, radius = 3.5f, center = Offset(x + cos(rot) * 24f, y + sin(rot) * 24f))
    }

    // Feathered Animated S1 / S2 / S3 Wings behind shoulders
    if (wingsTier > 0) {
        val flap = if (animationsEnabled) sin(nowMs / 130.0).toFloat() * 5f else 0f
        val wingColor = when (wingsTier) {
            3 -> Color(0xFFFBBF24)
            2 -> Color(0xFF38BDF8)
            else -> Color(0xFFA855F7)
        }
        val span = 24f + wingsTier * 4f
        drawPath(Path().apply {
            moveTo(x - 4f, y - 3f)
            lineTo(x - span, y - 16f + flap)
            lineTo(x - span * 0.85f, y + 4f + flap * 0.4f)
            lineTo(x - span * 0.45f, y + 9f)
            close()
        }, color = wingColor.copy(alpha = 0.82f))
        drawPath(Path().apply {
            moveTo(x + 4f, y - 3f)
            lineTo(x + span, y - 16f + flap)
            lineTo(x + span * 0.85f, y + 4f + flap * 0.4f)
            lineTo(x + span * 0.45f, y + 9f)
            close()
        }, color = wingColor.copy(alpha = 0.82f))
    }

    // DL / Equipped Pet (Dark Raven or Guardian Spirit)
    if (hasPet) {
        val orbit = if (animationsEnabled) (nowMs / 460.0).toFloat() else 0.7f
        val px = x + cos(orbit) * 24f
        val py = y - 18f + sin(orbit) * 7f
        drawCircle(color = Color(0xFF0F172A), radius = 5f, center = Offset(px, py))
        drawCircle(color = Color(0xFF38BDF8), radius = 3f, center = Offset(px, py))
    }

    // Ground shadow
    drawOval(color = Color.Black.copy(alpha = 0.48f), topLeft = Offset(x - 13f, y + 10f), size = Size(26f, 9f))

    // Cape behind DK/DL/MG
    if (classId == ClassId.DL || classId == ClassId.DK || classId == ClassId.MG) {
        drawPath(Path().apply {
            moveTo(x - 8f, y - 5f)
            lineTo(x + 8f, y - 5f)
            lineTo(x + 11f, y + 12f)
            lineTo(x - 11f, y + 12f)
            close()
        }, color = accent.copy(alpha = 0.55f))
    }

    // Boots & Greaves
    drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(x - 7f, y + 6f), size = Size(6f, 8f), cornerRadius = CornerRadius(2f, 2f))
    drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(x + 1f, y + 6f), size = Size(6f, 8f), cornerRadius = CornerRadius(2f, 2f))

    // Armored Torso & Shoulder Pauldrons
    drawRoundRect(color = accent, topLeft = Offset(x - 9f, y - 6f), size = Size(18f, 14f), cornerRadius = CornerRadius(4f, 4f))
    drawRoundRect(color = armorTrim, topLeft = Offset(x - 11f, y - 6f), size = Size(6f, 6f), cornerRadius = CornerRadius(2f, 2f))
    drawRoundRect(color = armorTrim, topLeft = Offset(x + 5f, y - 6f), size = Size(6f, 6f), cornerRadius = CornerRadius(2f, 2f))

    // Head
    drawCircle(color = Color(0xFFFDE68A), radius = 6f, center = Offset(x, y - 12f))

    // Distinct Class Headgear (MG strictly wears NO HELM!)
    when (classId) {
        ClassId.DK -> {
            drawArc(Color(0xFFCBD5E1), 180f, 180f, true, Offset(x - 6.5f, y - 19f), Size(13f, 10f))
            drawLine(Color(0xFFEF4444), Offset(x, y - 19f), Offset(x, y - 24f), strokeWidth = 2.5f)
        }
        ClassId.DW -> drawPath(Path().apply {
            moveTo(x - 8f, y - 15f)
            lineTo(x, y - 26f)
            lineTo(x + 8f, y - 15f)
            close()
        }, color = Color(0xFF2563EB))
        ClassId.ELF -> {
            drawCircle(Color(0xFF34D399), radius = 2.8f, center = Offset(x, y - 18f))
            drawLine(Color(0xFFFDE047), Offset(x - 5f, y - 16f), Offset(x + 5f, y - 16f), strokeWidth = 1.8f)
        }
        ClassId.DL -> drawRect(Color(0xFFFBBF24), topLeft = Offset(x - 6f, y - 20f), size = Size(12f, 4.5f))
        ClassId.MG -> {
            // Spiky exposed hair (NO HELMET per MU Online canon)
            drawPath(Path().apply {
                moveTo(x - 6f, y - 15f)
                lineTo(x - 3f, y - 21f)
                lineTo(x, y - 17f)
                lineTo(x + 3f, y - 21f)
                lineTo(x + 6f, y - 15f)
                close()
            }, color = Color(0xFF4C1D95))
        }
    }

    // Distinct Class Weapon
    val wx = x + sin(facingAngle) * 20f
    val wy = y + cos(facingAngle) * 20f
    val weaponColor = when (classId) {
        ClassId.DK -> Color(0xFFE2E8F0)
        ClassId.DW -> Color(0xFF38BDF8)
        ClassId.ELF -> Color(0xFF34D399)
        ClassId.MG -> Color(0xFFC084FC)
        ClassId.DL -> Color(0xFFFBBF24)
    }
    drawLine(color = weaponColor, start = Offset(x + 4f, y - 1f), end = Offset(wx, wy), strokeWidth = 3.8f, cap = StrokeCap.Round)
    if (classId == ClassId.DW || classId == ClassId.DL) {
        drawCircle(color = Color(0xFFFDE047), radius = 3.8f, center = Offset(wx, wy))
    }

    // Overhead HP bar & pill name
    val ratio = (hp.toFloat() / max(1, maxHp)).coerceIn(0f, 1f)
    drawRect(color = Color(0xFF0F172A), topLeft = Offset(x - 22f, y - 30f), size = Size(44f, 4.5f))
    drawRect(
        color = if (isLocal) Color(0xFF22C55E) else Color(0xFF38BDF8),
        topLeft = Offset(x - 22f, y - 30f),
        size = Size(44f * ratio, 4.5f)
    )
    drawPillLabel(
        text = displayName,
        cx = x,
        baselineY = y - 34f,
        textColorArgb = if (isLocal) Color(0xFFFDE68A).toArgb() else Color(0xFFBAE6FD).toArgb(),
        paint = labelPaint,
        pillPaint = pillPaint
    )
}
