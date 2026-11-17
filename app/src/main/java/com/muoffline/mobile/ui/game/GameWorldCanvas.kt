package com.muoffline.mobile.ui.game

import android.graphics.Paint
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
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
    val latestHero by rememberUpdatedState(hero)
    val latestNpcs by rememberUpdatedState(npcsOnMap)
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
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
        }
    }
    val subLabelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
        }
    }
    val floatPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val currentHero = latestHero
                    val wx = currentHero.x + (offset.x - size.width / 2f)
                    val wy = currentHero.y + (offset.y - size.height / 2f)
                    val npc = latestNpcs.firstOrNull {
                        hypot((it.wx - wx).toDouble(), (it.wy - wy).toDouble()) <= 36.0
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
        val camX = hero.x - size.width / 2f
        val camY = hero.y - size.height / 2f

        // 1. Background & Tiled Terrain
        drawRect(color = Color(mapMeta.groundPrimary))

        val tileSize = if (hdGraphics) 64f else 96f
        val startTileX = floor(camX / tileSize).toInt() - 1
        val endTileX = ceil((camX + size.width) / tileSize).toInt() + 1
        val startTileY = floor(camY / tileSize).toInt() - 1
        val endTileY = ceil((camY + size.height) / tileSize).toInt() + 1

        val primaryColor = Color(mapMeta.groundPrimary)
        val secondaryColor = Color(mapMeta.groundSecondary)
        val tileGapColor = Color.Black.copy(alpha = 0.22f)

        for (tx in startTileX..endTileX) {
            for (ty in startTileY..endTileY) {
                val wx = tx * tileSize
                val wy = ty * tileSize
                val sx = wx - camX
                val sy = wy - camY
                val inWorld = wx >= 0f && wy >= 0f && wx < MuWorldConstants.WORLD_SIZE && wy < MuWorldConstants.WORLD_SIZE
                if (!inWorld) {
                    drawRect(
                        color = Color(0xFF05070B),
                        topLeft = Offset(sx, sy),
                        size = Size(tileSize + 1f, tileSize + 1f)
                    )
                    continue
                }
                val checker = ((tx + ty) and 1) == 0
                drawRect(
                    color = if (checker) primaryColor else secondaryColor,
                    topLeft = Offset(sx, sy),
                    size = Size(tileSize + 0.5f, tileSize + 0.5f)
                )
                if (hdGraphics) {
                    drawRect(
                        color = tileGapColor,
                        topLeft = Offset(sx, sy),
                        size = Size(tileSize, tileSize),
                        style = Stroke(width = 1f)
                    )
                }
            }
        }

        // World boundary wall glow
        drawRect(
            color = Color(mapMeta.accentColor).copy(alpha = 0.5f),
            topLeft = Offset(-camX, -camY),
            size = Size(MuWorldConstants.WORLD_SIZE, MuWorldConstants.WORLD_SIZE),
            style = Stroke(width = 4f)
        )

        // 2. Safe Citadel & Landmark
        val safeSx = MuWorldConstants.SAFE_CENTER - camX
        val safeSy = MuWorldConstants.SAFE_CENTER - camY
        drawCircle(
            color = Color(mapMeta.safeZoneColor).copy(alpha = 0.6f),
            radius = MuWorldConstants.SAFE_RADIUS,
            center = Offset(safeSx, safeSy)
        )
        drawCircle(
            color = Color(mapMeta.accentColor).copy(alpha = 0.7f),
            radius = MuWorldConstants.SAFE_RADIUS,
            center = Offset(safeSx, safeSy),
            style = Stroke(width = 3f)
        )

        // Safe Citadel paths & Lorencia stone walls
        if (hero.mapId == MapId.LORENCIA) {
            // Stone roads leading out of Lorencia
            drawRect(color = Color(0xFF3E352B), topLeft = Offset(safeSx - 22f, safeSy - 260f), size = Size(44f, 520f))
            drawRect(color = Color(0xFF3E352B), topLeft = Offset(safeSx - 260f, safeSy - 22f), size = Size(520f, 44f))
            // Citadel stone ring markers
            drawCircle(
                color = Color(0xFF64748B).copy(alpha = 0.55f),
                radius = MuWorldConstants.SAFE_RADIUS + 8f,
                center = Offset(safeSx, safeSy),
                style = Stroke(width = 8f)
            )
        }

        if (hero.mapId == MapId.LORENCIA || hero.mapId == MapId.NORIA || hero.mapId == MapId.DEVIAS) {
            drawCircle(color = Color(0xFF475569), radius = 32f, center = Offset(safeSx, safeSy))
            val ripple = if (animationsEnabled) 14f + sin(now / 320.0).toFloat() * 4f else 15f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(mapMeta.accentColor), Color(0xFF0F172A)),
                    center = Offset(safeSx, safeSy),
                    radius = 22f
                ),
                radius = 22f,
                center = Offset(safeSx, safeSy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = ripple,
                center = Offset(safeSx, safeSy),
                style = Stroke(width = 2f)
            )
        }

        if (hdGraphics && hero.mapId == MapId.LORENCIA) {
            val torchOffsets = listOf(
                Offset(-84f, -84f), Offset(84f, -84f),
                Offset(-84f, 84f), Offset(84f, 84f)
            )
            torchOffsets.forEachIndexed { idx, off ->
                val tx = safeSx + off.x
                val ty = safeSy + off.y
                val flicker = if (animationsEnabled) 1f + sin(now / 110.0 + idx).toFloat() * 0.16f else 1f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFF59E0B).copy(alpha = 0.36f), Color.Transparent),
                        center = Offset(tx, ty),
                        radius = 44f * flicker
                    ),
                    radius = 44f * flicker,
                    center = Offset(tx, ty)
                )
                drawCircle(color = Color(0xFF334155), radius = 9f, center = Offset(tx, ty))
                drawCircle(color = Color(0xFFFBBF24), radius = 5.5f * flicker, center = Offset(tx, ty))
            }
        }

        // Dynamic NPCs for current map with stalls & recognizable RPG icons
        for (npc in npcsOnMap) {
            val nx = npc.wx - camX
            val ny = npc.wy - camY
            if (nx < -90f || nx > size.width + 90f || ny < -90f || ny > size.height + 90f) continue
            val npcColor = when (npc.role) {
                "chaos" -> Color(0xFFFBBF24)
                "archer" -> Color(0xFF38BDF8)
                "vault" -> Color(0xFF94A3B8)
                "quest" -> Color(0xFFA855F7)
                else -> Color(0xFF10B981)
            }
            // Pedestal / market stall awning
            drawRoundRect(
                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                topLeft = Offset(nx - 22f, ny - 24f),
                size = Size(44f, 44f),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = npcColor.copy(alpha = 0.7f),
                topLeft = Offset(nx - 22f, ny - 24f),
                size = Size(44f, 10f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Ground aura
            drawCircle(color = npcColor.copy(alpha = 0.25f), radius = 26f, center = Offset(nx, ny))
            // Body cloak + shoulders
            drawRoundRect(
                color = npcColor,
                topLeft = Offset(nx - 11f, ny - 4f),
                size = Size(22f, 20f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Head
            drawCircle(color = Color(0xFFFDE68A), radius = 7.5f, center = Offset(nx, ny - 11f))
            // Role emblem
            when (npc.role) {
                "chaos" -> {
                    drawCircle(color = Color(0xFFF59E0B), radius = 5f, center = Offset(nx + 14f, ny + 4f))
                }
                "vault" -> {
                    drawRect(color = Color(0xFFFBBF24), topLeft = Offset(nx + 10f, ny), size = Size(10f, 9f))
                }
                "quest" -> {
                    drawCircle(color = Color(0xFFFDE047), radius = 5f, center = Offset(nx, ny - 22f))
                }
            }
            drawIntoCanvas { canvas ->
                labelPaint.color = Color(0xFFFDE68A).toArgb()
                canvas.nativeCanvas.drawText(npc.name, nx, ny - 32f, labelPaint)
                subLabelPaint.color = Color(0xFFCBD5E1).toArgb()
                canvas.nativeCanvas.drawText(npc.subtitle, nx, ny + 34f, subLabelPaint)
            }
        }

        // 3. Ground Drops
        for (drop in drops) {
            val dx = drop.x - camX
            val dy = drop.y - camY
            if (dx < -40f || dx > size.width + 40f || dy < -40f || dy > size.height + 40f) continue
            val pulse = if (animationsEnabled) 1f + sin(now / 180.0).toFloat() * 0.14f else 1f
            if (drop.zen != null) {
                drawCircle(color = Color(0xFFFBBF24).copy(alpha = 0.28f), radius = 15f * pulse, center = Offset(dx, dy))
                drawCircle(color = Color(0xFFF59E0B), radius = 7f, center = Offset(dx, dy))
                drawIntoCanvas { canvas ->
                    subLabelPaint.color = Color(0xFFFDE68A).toArgb()
                    canvas.nativeCanvas.drawText("${drop.zen} Zen", dx, dy - 16f, subLabelPaint)
                }
            } else if (drop.item != null) {
                val c = Color(drop.item.colorHex)
                drawCircle(color = c.copy(alpha = 0.30f), radius = 17f * pulse, center = Offset(dx, dy))
                drawRoundRect(
                    color = c,
                    topLeft = Offset(dx - 7f, dy - 7f),
                    size = Size(14f, 14f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawIntoCanvas { canvas ->
                    subLabelPaint.color = c.toArgb()
                    canvas.nativeCanvas.drawText(drop.item.name, dx, dy - 18f, subLabelPaint)
                }
            }
        }

        // 4. Monsters with family silhouettes
        for (m in monsters) {
            if (m.hp <= 0) continue
            val mx = m.x - camX
            val my = m.y - camY
            if (mx < -80f || mx > size.width + 80f || my < -80f || my > size.height + 80f) continue
            val flashing = now < m.hitFlashUntil
            val bodyColor = if (flashing) Color.White else Color(m.colorHex)

            if (m.isBoss) {
                drawCircle(
                    color = Color(0xFFFBBF24).copy(alpha = 0.32f),
                    radius = m.radius + 14f,
                    center = Offset(mx, my),
                    style = Stroke(width = 3f)
                )
            }
            drawOval(
                color = Color.Black.copy(alpha = 0.42f),
                topLeft = Offset(mx - m.radius * 0.85f, my + m.radius * 0.48f),
                size = Size(m.radius * 1.7f, m.radius * 0.65f)
            )
            // Monster legs / wings / horns by type
            val lower = m.templateId.lowercase()
            if (lower.contains("spider")) {
                for (leg in -1..1) {
                    val offY = leg * 5f
                    drawLine(bodyColor, Offset(mx - m.radius - 7f, my + offY), Offset(mx + m.radius + 7f, my - offY), strokeWidth = 2.5f)
                }
            } else if (lower.contains("dragon") || lower.contains("phoenix") || lower.contains("hydra")) {
                val wingSpan = m.radius * 1.55f
                drawPath(Path().apply {
                    moveTo(mx, my)
                    lineTo(mx - wingSpan, my - m.radius * 0.8f)
                    lineTo(mx - wingSpan * 0.65f, my + m.radius * 0.35f)
                    close()
                }, color = bodyColor.copy(alpha = 0.65f))
                drawPath(Path().apply {
                    moveTo(mx, my)
                    lineTo(mx + wingSpan, my - m.radius * 0.8f)
                    lineTo(mx + wingSpan * 0.65f, my + m.radius * 0.35f)
                    close()
                }, color = bodyColor.copy(alpha = 0.65f))
            } else if (lower.contains("bull") || lower.contains("balrog") || lower.contains("kundun")) {
                drawLine(Color(0xFFFDE68A), Offset(mx - 6f, my - m.radius * 0.6f), Offset(mx - 14f, my - m.radius * 1.25f), strokeWidth = 3.5f)
                drawLine(Color(0xFFFDE68A), Offset(mx + 6f, my - m.radius * 0.6f), Offset(mx + 14f, my - m.radius * 1.25f), strokeWidth = 3.5f)
            }

            drawCircle(color = bodyColor, radius = m.radius, center = Offset(mx, my))
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = m.radius,
                center = Offset(mx, my),
                style = Stroke(width = 2.5f)
            )
            drawCircle(color = Color(0xFFFEF08A), radius = 3.2f, center = Offset(mx - m.radius * 0.32f, my - m.radius * 0.15f))
            drawCircle(color = Color(0xFFFEF08A), radius = 3.2f, center = Offset(mx + m.radius * 0.32f, my - m.radius * 0.15f))

            val barW = max(44f, m.radius * 2.4f)
            val barTop = my - m.radius - 14f
            drawRect(color = Color(0xFF0F172A), topLeft = Offset(mx - barW / 2f, barTop), size = Size(barW, 6f))
            val hpRatio = (m.hp.toFloat() / max(1, m.maxHp)).coerceIn(0f, 1f)
            drawRect(
                color = if (m.isBoss) Color(0xFFF59E0B) else Color(0xFFEF4444),
                topLeft = Offset(mx - barW / 2f, barTop),
                size = Size(barW * hpRatio, 6f)
            )
            drawIntoCanvas { canvas ->
                subLabelPaint.color = if (m.isBoss) Color(0xFFFBBF24).toArgb() else Color.White.toArgb()
                canvas.nativeCanvas.drawText("${m.name} Nv.${m.level}", mx, barTop - 5f, subLabelPaint)
            }
        }

        // 5. Cooperative LAN Bosses on current map
        for (lb in lanBosses) {
            if (lb.mapId != hero.mapId || lb.hp <= 0) continue
            val bx = lb.x - camX
            val by = lb.y - camY
            val pulse = if (animationsEnabled) 36f + sin(now / 150.0).toFloat() * 4f else 36f
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.32f),
                radius = pulse + 10f,
                center = Offset(bx, by),
                style = Stroke(width = 4f)
            )
            drawCircle(color = Color(0xFF0284C7), radius = 34f, center = Offset(bx, by))
            drawCircle(color = Color(0xFFFBBF24), radius = 34f, center = Offset(bx, by), style = Stroke(width = 3f))
            val ratio = (lb.hp.toFloat() / max(1, lb.maxHp)).coerceIn(0f, 1f)
            drawRect(color = Color(0xFF0F172A), topLeft = Offset(bx - 42f, by - 48f), size = Size(84f, 8f))
            drawRect(color = Color(0xFF38BDF8), topLeft = Offset(bx - 42f, by - 48f), size = Size(84f * ratio, 8f))
            drawIntoCanvas { canvas ->
                labelPaint.color = Color(0xFF38BDF8).toArgb()
                canvas.nativeCanvas.drawText("[BOSS LAN] ${lb.name}", bx, by - 55f, labelPaint)
            }
        }

        // 6. Remote LAN Players
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
                displayName = "${lp.name} [Nv.${lp.level}]",
                nowMs = now,
                animationsEnabled = animationsEnabled,
                isLocal = false,
                labelPaint = labelPaint
            )
        }

        // 7. Local Hero Avatar
        val heroWingsTier = hero.equipment.wings?.wingsTier?.coerceAtLeast(1) ?: 0
        drawHeroAvatar(
            x = size.width / 2f,
            y = size.height / 2f,
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
            labelPaint = labelPaint
        )

        // 8. Skill VFX
        if (animationsEnabled) {
            for (vfx in vfxList) {
                val age = (now - vfx.createdAt).coerceAtLeast(0L)
                val progress = (age.toFloat() / max(1L, vfx.durationMs)).coerceIn(0f, 1f)
                val alpha = (1f - progress) * 0.85f
                val sx = vfx.x - camX
                val sy = vfx.y - camY
                val tx = vfx.targetX - camX
                val ty = vfx.targetY - camY
                val c = Color(vfx.colorHex).copy(alpha = alpha)

                when (vfx.skillId) {
                    "evil-spirit", "twisting-slash", "rageful-blow", "ice-storm", "chaotic-diseier", "flame-strike" -> {
                        val curRadius = vfx.radius * (0.35f + progress * 0.75f)
                        drawCircle(color = c, radius = curRadius, center = Offset(sx, sy), style = Stroke(width = 6f))
                        drawCircle(color = c.copy(alpha = alpha * 0.25f), radius = curRadius * 0.85f, center = Offset(sx, sy))
                        if (hdGraphics) {
                            for (orb in 0..3) {
                                val ang = progress * 6.28f + orb * 1.57f
                                drawCircle(
                                    color = Color.White.copy(alpha = alpha),
                                    radius = 5f,
                                    center = Offset(sx + cos(ang) * curRadius * 0.7f, sy + sin(ang) * curRadius * 0.7f)
                                )
                            }
                        }
                    }
                    "heal-aura" -> {
                        drawCircle(
                            color = Color(0xFF34D399).copy(alpha = alpha * 0.45f),
                            radius = vfx.radius * (0.4f + progress * 0.6f),
                            center = Offset(sx, sy)
                        )
                    }
                    else -> {
                        drawLine(color = c, start = Offset(sx, sy), end = Offset(tx, ty), strokeWidth = 6f)
                        drawCircle(color = c.copy(alpha = alpha * 0.45f), radius = vfx.radius * progress, center = Offset(tx, ty))
                    }
                }
            }
        }

        // 9. Floating Damage / Loot Text
        drawIntoCanvas { canvas ->
            for (ft in floatingTexts) {
                val age = (now - ft.createdAt).coerceAtLeast(0L)
                val p = (age / 950f).coerceIn(0f, 1f)
                val fx = ft.x - camX
                val fy = (ft.y - camY) - p * 34f
                val alphaInt = ((1f - p) * 255).toInt().coerceIn(0, 255)
                val baseColor = Color(ft.colorHex)
                floatPaint.textSize = if (ft.isCrit) 30f else 24f
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
    labelPaint: Paint
) {
    val accent = Color(MuCatalog.CLASS_CATALOG.getValue(classId).accentColor)

    if (weaponPlus >= 7) {
        val rot = if (animationsEnabled) (nowMs % 1600L) / 1600f * 360f else 0f
        val auraColor = if (weaponPlus >= 11) Color(0xFF38BDF8) else Color(0xFFFBBF24)
        drawCircle(
            color = auraColor.copy(alpha = 0.45f),
            radius = 29f,
            center = Offset(x, y),
            style = Stroke(width = 3f)
        )
        val rad = Math.toRadians(rot.toDouble())
        drawCircle(
            color = auraColor,
            radius = 4f,
            center = Offset(x + (cos(rad) * 29f).toFloat(), y + (sin(rad) * 29f).toFloat())
        )
    }

    if (wingsTier > 0) {
        val flap = if (animationsEnabled) sin(nowMs / 130.0).toFloat() * 6f else 0f
        val wingColor = when (wingsTier) {
            3 -> Color(0xFFFBBF24)
            2 -> Color(0xFF38BDF8)
            else -> Color(0xFFA855F7)
        }
        val span = 30f + wingsTier * 4f
        val leftWing = Path().apply {
            moveTo(x - 6f, y)
            lineTo(x - span, y - 18f + flap)
            lineTo(x - span * 0.72f, y + 10f)
            close()
        }
        val rightWing = Path().apply {
            moveTo(x + 6f, y)
            lineTo(x + span, y - 18f + flap)
            lineTo(x + span * 0.72f, y + 10f)
            close()
        }
        drawPath(leftWing, color = wingColor.copy(alpha = 0.78f))
        drawPath(rightWing, color = wingColor.copy(alpha = 0.78f))
    }

    if (hasPet) {
        val orbit = if (animationsEnabled) (nowMs / 500.0).toFloat() else 0.7f
        val px = x + cos(orbit) * 32f
        val py = y - 18f + sin(orbit) * 10f
        drawCircle(color = Color(0xFF38BDF8), radius = 6.5f, center = Offset(px, py))
    }

    drawOval(
        color = Color.Black.copy(alpha = 0.45f),
        topLeft = Offset(x - 16f, y + 12f),
        size = Size(32f, 12f)
    )

    // Armored torso + shoulders + head
    drawRoundRect(
        color = accent,
        topLeft = Offset(x - 12f, y - 4f),
        size = Size(24f, 20f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawCircle(color = Color(0xFFFDE68A), radius = 8f, center = Offset(x, y - 11f))
    // Class headgear (MG intentionally has no helmet!)
    when (classId) {
        ClassId.DK -> drawArc(Color(0xFFCBD5E1), 180f, 180f, true, Offset(x - 8f, y - 20f), Size(16f, 12f))
        ClassId.DW -> drawPath(Path().apply {
            moveTo(x - 10f, y - 15f)
            lineTo(x, y - 28f)
            lineTo(x + 10f, y - 15f)
            close()
        }, color = Color(0xFF2563EB))
        ClassId.ELF -> drawCircle(Color(0xFF34D399), radius = 3.5f, center = Offset(x, y - 19f))
        ClassId.DL -> drawRect(Color(0xFFFBBF24), topLeft = Offset(x - 7f, y - 22f), size = Size(14f, 5f))
        ClassId.MG -> Unit // Magic Gladiator never wears helm
    }

    val wx = x + sin(facingAngle) * 24f
    val wy = y + cos(facingAngle) * 24f
    drawLine(
        color = when (classId) {
            ClassId.DK -> Color(0xFFE2E8F0)
            ClassId.DW -> Color(0xFF38BDF8)
            ClassId.ELF -> Color(0xFF34D399)
            ClassId.MG -> Color(0xFFC084FC)
            ClassId.DL -> Color(0xFFFBBF24)
        },
        start = Offset(x, y),
        end = Offset(wx, wy),
        strokeWidth = 5f
    )
    if (classId == ClassId.DW || classId == ClassId.DL) {
        drawCircle(color = Color(0xFFFBBF24), radius = 4.5f, center = Offset(wx, wy))
    }

    val ratio = (hp.toFloat() / max(1, maxHp)).coerceIn(0f, 1f)
    drawRect(color = Color(0xFF0F172A), topLeft = Offset(x - 26f, y - 34f), size = Size(52f, 5f))
    drawRect(
        color = if (isLocal) Color(0xFF22C55E) else Color(0xFF38BDF8),
        topLeft = Offset(x - 26f, y - 34f),
        size = Size(52f * ratio, 5f)
    )
    drawIntoCanvas { canvas ->
        labelPaint.color = if (isLocal) Color(0xFFFDE68A).toArgb() else Color(0xFFBAE6FD).toArgb()
        canvas.nativeCanvas.drawText(displayName, x, y - 41f, labelPaint)
    }
}
